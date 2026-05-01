package com.focusguard.state

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusguard.service.AccessibilityServiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

private val Context.earnedItDataStore by preferencesDataStore("earnedit_state")
private val stateJsonKey = stringPreferencesKey("state_json_v1")
private const val InitialPetFullness = 50
private const val InitialPetMood = "Ready"

data class PetProfile(
    val name: String = "Kitsu",
    val species: String = "kitsu",
    val stage: Int = 1,
    val fullness: Int = InitialPetFullness,
    val mood: String = InitialPetMood,
    val lastFedMs: Long = 0L,
    val equippedCosmetic: String = ""
)

data class UserProfile(
    val displayName: String = "Sanjiv",
    val username: String = "sual"
)

data class FocusSessionSummary(
    val id: Long,
    val durationMinutes: Int,
    val focusScore: Int,
    val success: Boolean,
    val pointsEarned: Int,
    val distractionCount: Int,
    val startTimeMs: Long = id,
    val endTimeMs: Long = id,
    val plannedDurationMinutes: Int = durationMinutes,
    val endedEarly: Boolean = false,
    val blockedApps: List<String> = emptyList(),
    val timeBankMinutesEarned: Int = 0,
    val recoverySeconds: Int = 0
)

data class TimeBankTransaction(
    val id: Long,
    val type: String,
    val minutes: Int,
    val title: String,
    val detail: String,
    val timestampMs: Long,
    val rewardApp: String = "",
    val expiresAtMs: Long = 0L
)

data class StorePurchase(
    val id: Long,
    val itemId: String,
    val itemName: String,
    val category: String,
    val pointsSpent: Int,
    val timestampMs: Long,
    val equipped: Boolean = false
)

data class ScheduledFocusBlock(
    val id: Long,
    val title: String,
    val startTimeMs: Long,
    val durationMinutes: Int,
    val blockedApps: List<String>,
    val reminderMinutes: Int = 10,
    val status: String = "scheduled"
)

data class PrivacyEvent(
    val id: Long,
    val type: String,
    val title: String,
    val detail: String,
    val timestampMs: Long
)

data class DeskAuditSummary(
    val score: Int = 0,
    val lightingScore: Int = 0,
    val clutterScore: Int = 0,
    val phoneRiskScore: Int = 0,
    val postureScore: Int = 0,
    val timestampMs: Long = 0L
)

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val strictBlockingEnabled: Boolean = false,
    val demoModeEnabled: Boolean = false,
    val theme: String = "system"
)

data class PermissionSnapshot(
    val accessibilityEnabled: Boolean = false,
    val cameraGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val usageAccessGranted: Boolean = false
)

data class EarnedItUiState(
    val onboardingComplete: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val points: Int = 0,
    val pet: PetProfile = PetProfile(),
    val allSessions: List<FocusSessionSummary> = emptyList(),
    val timeBankTransactions: List<TimeBankTransaction> = emptyList(),
    val storePurchases: List<StorePurchase> = emptyList(),
    val scheduledBlocks: List<ScheduledFocusBlock> = emptyList(),
    val privacyEvents: List<PrivacyEvent> = emptyList(),
    val deskAudit: DeskAuditSummary = DeskAuditSummary(),
    val settings: AppSettings = AppSettings(),
    val permissions: PermissionSnapshot = PermissionSnapshot(),
    val lastSession: FocusSessionSummary? = null,
    val loaded: Boolean = false
) {
    val sessionsToday: List<FocusSessionSummary>
        get() = allSessions.filter { isSameLocalDay(it.startTimeMs, System.currentTimeMillis()) }

    val focusMinutesToday: Int
        get() = sessionsToday.filter { it.success }.sumOf { it.durationMinutes }

    val weekSessions: List<FocusSessionSummary>
        get() = allSessions.filter { isSameLocalWeek(it.startTimeMs, System.currentTimeMillis()) }

    val weeklyFocusMinutes: Int
        get() = weekSessions.filter { it.success }.sumOf { it.durationMinutes }

    val averageFocusScore: Int
        get() = allSessions.takeIf { it.isNotEmpty() }?.map { it.focusScore }?.average()?.roundToInt() ?: 0

    val bestSession: FocusSessionSummary?
        get() = allSessions.maxWithOrNull(compareBy<FocusSessionSummary> { it.focusScore }.thenBy { it.durationMinutes })

    val timeBankMinutes: Int
        get() = timeBankTransactions.sumOf { it.minutes }.coerceAtLeast(0)

    val streakDays: Int
        get() = calculateStreakDays(allSessions)

    val lifetimeFocusMinutes: Int
        get() = allSessions.filter { it.success }.sumOf { it.durationMinutes }
}

object EarnedItStore {
    private val _state = MutableStateFlow(EarnedItUiState())
    val state: StateFlow<EarnedItUiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        scope.launch {
            context.applicationContext.earnedItDataStore.data
                .catch {
                    _state.value = EarnedItUiState(loaded = true)
                }
                .collect { preferences ->
                    val stored = preferences[stateJsonKey]
                    _state.value = if (stored.isNullOrBlank()) {
                        EarnedItUiState(loaded = true, permissions = readPermissions(context.applicationContext))
                    } else {
                        runCatching {
                            EarnedItJson.decode(stored)
                                .withFreshPetBaseline()
                                .copy(loaded = true, permissions = readPermissions(context.applicationContext))
                        }
                            .getOrElse { EarnedItUiState(loaded = true, permissions = readPermissions(context.applicationContext)) }
                    }
                }
        }
    }

    fun finalizeOnboarding() {
        mutate { it.copy(onboardingComplete = true).withPrivacyEvent("settings", "Onboarding complete", "Initial local profile created.") }
    }

    fun replayOnboardingForPreview() {
        mutate { it.copy(onboardingComplete = false).withPrivacyEvent("settings", "Onboarding replay", "Temporary preview flow reopened from Home.") }
    }

    fun pickPet(species: String, name: String) {
        mutate { state ->
            state.copy(
                pet = state.pet.copy(
                    species = species,
                    name = name.ifBlank { species.replaceFirstChar { c -> c.uppercase() } },
                    stage = 1,
                    fullness = InitialPetFullness,
                    mood = InitialPetMood,
                    lastFedMs = 0L,
                    equippedCosmetic = ""
                )
            ).withPrivacyEvent("pet", "Pet selected", "Your local pet profile was updated.")
        }
    }

    fun recordSession(durationSeconds: Int, focusScore: Float, distractionCount: Int, endedEarly: Boolean) {
        val now = System.currentTimeMillis()
        val durationMinutes = (durationSeconds / 60f).roundToInt().coerceAtLeast(1)
        val score = focusScore.roundToInt().coerceIn(0, 100)
        val success = !endedEarly && score >= 70
        val points = if (success) {
            (durationMinutes * 3 + score / 2 + if (distractionCount == 0) 25 else 0)
        } else {
            0
        }
        val bankMinutes = if (success) (durationMinutes / 5).coerceAtLeast(1) else 0
        mutate { state ->
            val session = FocusSessionSummary(
                id = now,
                durationMinutes = durationMinutes,
                plannedDurationMinutes = durationMinutes,
                focusScore = score,
                success = success,
                pointsEarned = points,
                distractionCount = distractionCount,
                startTimeMs = now - durationMinutes * 60_000L,
                endTimeMs = now,
                endedEarly = endedEarly,
                blockedApps = listOf("Instagram", "TikTok"),
                timeBankMinutesEarned = bankMinutes,
                recoverySeconds = if (distractionCount == 0) 0 else (35 + distractionCount * 12)
            )
            val transaction = TimeBankTransaction(
                id = now + 1,
                type = "earn",
                minutes = bankMinutes,
                title = "+${bankMinutes}m earned",
                detail = "${durationMinutes} minute session at ${score}% focus.",
                timestampMs = now
            )
            val nextStage = ((state.lifetimeFocusMinutes + durationMinutes) / 120 + 1).coerceIn(1, 5)
            val updated = state.copy(
                points = state.points + points,
                allSessions = (listOf(session) + state.allSessions).take(250),
                timeBankTransactions = (if (bankMinutes > 0) listOf(transaction) else emptyList()) + state.timeBankTransactions,
                pet = state.pet.copy(
                    stage = nextStage,
                    fullness = (state.pet.fullness + if (success) 8 else -6).coerceIn(0, 100),
                    mood = when {
                        success && score >= 90 -> "Energized"
                        success -> "Happy"
                        else -> "Bored"
                    }
                ),
                lastSession = session
            )
            updated.withPrivacyEvent("session", "Attention score calculated", "${score}% focus score saved locally.")
        }
    }

    fun feedPet(): Boolean {
        val current = _state.value
        if (current.points < 25) return false
        mutate { state ->
            state.copy(
                points = state.points - 25,
                pet = state.pet.copy(fullness = (state.pet.fullness + 14).coerceAtMost(100), mood = "Full and focused", lastFedMs = System.currentTimeMillis())
            ).withPrivacyEvent("pet", "Pet fed", "Spent 25 points on pet care.")
        }
        return true
    }

    fun playWithPet() {
        mutate { state ->
            state.copy(
                pet = state.pet.copy(fullness = (state.pet.fullness - 4).coerceAtLeast(0), mood = "Playful")
            ).withPrivacyEvent("pet", "Pet played", "Mood updated locally.")
        }
    }

    fun encouragePet() {
        mutate { state ->
            state.copy(pet = state.pet.copy(mood = "Brave"))
                .withPrivacyEvent("pet", "Pet encouraged", "Mood updated locally.")
        }
    }

    fun purchaseStoreItem(itemId: String, itemName: String, category: String, price: Int): PurchaseResult {
        val current = _state.value
        if (current.storePurchases.any { it.itemId == itemId }) return PurchaseResult.AlreadyOwned
        if (current.points < price) return PurchaseResult.InsufficientPoints
        val now = System.currentTimeMillis()
        mutate { state ->
            val purchase = StorePurchase(now, itemId, itemName, category, price, now, equipped = category == "Pet")
            val updatedPet = if (category == "Pet") state.pet.copy(equippedCosmetic = itemName) else state.pet
            state.copy(
                points = state.points - price,
                storePurchases = listOf(purchase) + state.storePurchases,
                pet = updatedPet
            ).withPrivacyEvent("store", "Store purchase", "$itemName purchased locally for $price points.")
        }
        return PurchaseResult.Success
    }

    fun redeemTimeBank(minutes: Int, rewardApp: String): Boolean {
        if (_state.value.timeBankMinutes < minutes) return false
        val now = System.currentTimeMillis()
        mutate { state ->
            val transaction = TimeBankTransaction(
                id = now,
                type = "redeem",
                minutes = -minutes,
                title = "-${minutes}m reserved",
                detail = "$rewardApp reward window created.",
                timestampMs = now,
                rewardApp = rewardApp,
                expiresAtMs = now + minutes * 60_000L
            )
            state.copy(timeBankTransactions = listOf(transaction) + state.timeBankTransactions)
                .withPrivacyEvent("reward", "Reward time redeemed", "$minutes minutes reserved for $rewardApp.")
        }
        return true
    }

    fun scheduleFocusBlock(title: String, durationMinutes: Int) {
        val now = System.currentTimeMillis()
        val start = now + 30 * 60_000L
        mutate { state ->
            val block = ScheduledFocusBlock(
                id = now,
                title = title,
                startTimeMs = start,
                durationMinutes = durationMinutes,
                blockedApps = listOf("Instagram", "TikTok")
            )
            state.copy(scheduledBlocks = (listOf(block) + state.scheduledBlocks).take(50))
                .withPrivacyEvent("calendar", "Focus block scheduled", "$durationMinutes minute block saved locally.")
        }
    }

    fun runDeskAudit() {
        val now = System.currentTimeMillis()
        mutate { state ->
            state.copy(
                deskAudit = DeskAuditSummary(
                    score = 87,
                    lightingScore = 92,
                    clutterScore = 64,
                    phoneRiskScore = 38,
                    postureScore = 84,
                    timestampMs = now
                )
            ).withPrivacyEvent("audit", "Desk audit run", "Workspace score saved without storing frames.")
        }
    }

    fun updateSettings(settings: AppSettings) {
        mutate { state ->
            state.copy(settings = settings)
                .withPrivacyEvent("settings", "Settings updated", "Preference changes saved locally.")
        }
    }

    fun clearSessionHistory() {
        mutate { state ->
            state.copy(
                allSessions = emptyList(),
                timeBankTransactions = emptyList(),
                lastSession = null
            ).withPrivacyEvent("privacy", "Session history cleared", "Local session and reward ledgers were cleared.")
        }
    }

    fun resetDemoData() {
        mutate { seedDemoState().copy(onboardingComplete = true, loaded = true) }
    }

    private fun mutate(reducer: (EarnedItUiState) -> EarnedItUiState) {
        _state.update { reducer(it).copy(loaded = true) }
        persist()
    }

    private fun persist() {
        val context = appContext ?: return
        val snapshot = _state.value
        scope.launch {
            context.earnedItDataStore.edit { preferences ->
                preferences[stateJsonKey] = EarnedItJson.encode(snapshot)
            }
        }
    }
}

enum class PurchaseResult {
    Success,
    AlreadyOwned,
    InsufficientPoints,
    Locked
}

fun seedDemoState(): EarnedItUiState {
    val now = System.currentTimeMillis()
    val sessions = listOf(
        FocusSessionSummary(now - 3_600_000L, 45, 91, true, 140, 1, now - 6_300_000L, now - 3_600_000L, 45, false, listOf("Instagram"), 9, 42),
        FocusSessionSummary(now - 86_400_000L, 25, 84, true, 105, 2, now - 87_900_000L, now - 86_400_000L, 25, false, listOf("TikTok"), 5, 58),
        FocusSessionSummary(now - 172_800_000L, 50, 94, true, 175, 0, now - 175_800_000L, now - 172_800_000L, 50, false, listOf("YouTube"), 10, 0)
    )
    return EarnedItUiState(
        onboardingComplete = true,
        profile = UserProfile("Sanjiv", "sual"),
        points = 2840,
        pet = PetProfile(name = "Kitsu", species = "kitsu", stage = 3, fullness = 78, mood = "Happy"),
        allSessions = sessions,
        timeBankTransactions = listOf(
            TimeBankTransaction(now - 3_600_000L, "earn", 9, "+9m earned", "45 minute session at 91% focus.", now - 3_600_000L),
            TimeBankTransaction(now - 5_400_000L, "redeem", -15, "-15m reserved", "YouTube reward pass after homework.", now - 5_400_000L, "YouTube"),
            TimeBankTransaction(now - 86_400_000L, "earn", 5, "+5m earned", "25 minute session at 84% focus.", now - 86_400_000L)
        ),
        storePurchases = listOf(StorePurchase(now - 120_000L, "lumi_scarf", "Lumi scarf", "Pet", 260, now - 120_000L, true)),
        scheduledBlocks = listOf(
            ScheduledFocusBlock(now + 1_800_000L, "Focus block", now + 1_800_000L, 25, listOf("Instagram", "TikTok")),
            ScheduledFocusBlock(now + 86_400_000L, "Algorithms set", now + 86_400_000L, 50, listOf("YouTube", "Discord"))
        ),
        privacyEvents = listOf(
            PrivacyEvent(now - 60_000L, "session", "Attention score calculated", "91% focus score saved locally.", now - 60_000L),
            PrivacyEvent(now - 120_000L, "reward", "Reward app check", "TikTok blocked during focus.", now - 120_000L)
        ),
        deskAudit = DeskAuditSummary(87, 92, 64, 38, 84, now - 600_000L),
        settings = AppSettings(demoModeEnabled = true),
        loaded = true
    )
}

private fun EarnedItUiState.withPrivacyEvent(type: String, title: String, detail: String): EarnedItUiState {
    val now = System.currentTimeMillis()
    return copy(
        privacyEvents = (listOf(PrivacyEvent(now, type, title, detail, now)) + privacyEvents).take(80)
    )
}

private fun EarnedItUiState.withFreshPetBaseline(): EarnedItUiState {
    val hasProgress = points > 0 ||
        allSessions.isNotEmpty() ||
        timeBankTransactions.isNotEmpty() ||
        storePurchases.isNotEmpty() ||
        pet.lastFedMs > 0L ||
        pet.stage > 1 ||
        pet.equippedCosmetic.isNotBlank()

    val looksLikeOldFreshPet = !hasProgress &&
        pet.fullness == 84 &&
        pet.mood == "Happy"

    return if (looksLikeOldFreshPet) {
        copy(pet = pet.copy(fullness = InitialPetFullness, mood = InitialPetMood))
    } else {
        this
    }
}

private fun calculateStreakDays(sessions: List<FocusSessionSummary>): Int {
    val successDays = sessions.filter { it.success }
        .map { Instant.ofEpochMilli(it.startTimeMs).atZone(ZoneId.systemDefault()).toLocalDate() }
        .toSet()
    if (successDays.isEmpty()) return 0
    var cursor = LocalDate.now()
    if (cursor !in successDays) {
        cursor = cursor.minusDays(1)
        if (cursor !in successDays) return 0
    }
    var count = 0
    while (cursor in successDays) {
        count += 1
        cursor = cursor.minusDays(1)
    }
    return count
}

private fun isSameLocalDay(a: Long, b: Long): Boolean {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(a).atZone(zone).toLocalDate() == Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
}

private fun isSameLocalWeek(a: Long, b: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val fields = WeekFields.of(Locale.getDefault())
    val aDate = Instant.ofEpochMilli(a).atZone(zone).toLocalDate()
    val bDate = Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
    return aDate.get(fields.weekBasedYear()) == bDate.get(fields.weekBasedYear()) &&
        aDate.get(fields.weekOfWeekBasedYear()) == bDate.get(fields.weekOfWeekBasedYear())
}

private fun readPermissions(context: Context): PermissionSnapshot {
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val usageMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return PermissionSnapshot(
        accessibilityEnabled = AccessibilityServiceStatus.isEnabled(context),
        cameraGranted = cameraGranted,
        notificationsGranted = notificationsGranted,
        usageAccessGranted = usageMode == AppOpsManager.MODE_ALLOWED
    )
}

private object EarnedItJson {
    fun encode(state: EarnedItUiState): String = JSONObject()
        .put("onboardingComplete", state.onboardingComplete)
        .put("profile", encodeProfile(state.profile))
        .put("points", state.points)
        .put("pet", encodePet(state.pet))
        .put("sessions", JSONArray(state.allSessions.map(::encodeSession)))
        .put("timeBankTransactions", JSONArray(state.timeBankTransactions.map(::encodeTransaction)))
        .put("storePurchases", JSONArray(state.storePurchases.map(::encodePurchase)))
        .put("scheduledBlocks", JSONArray(state.scheduledBlocks.map(::encodeBlock)))
        .put("privacyEvents", JSONArray(state.privacyEvents.map(::encodePrivacyEvent)))
        .put("deskAudit", encodeDeskAudit(state.deskAudit))
        .put("settings", encodeSettings(state.settings))
        .put("lastSession", state.lastSession?.let(::encodeSession) ?: JSONObject.NULL)
        .toString()

    fun decode(json: String): EarnedItUiState {
        val objectJson = JSONObject(json)
        return EarnedItUiState(
            onboardingComplete = objectJson.optBoolean("onboardingComplete", false),
            profile = decodeProfile(objectJson.optJSONObject("profile")),
            points = objectJson.optInt("points", 0),
            pet = decodePet(objectJson.optJSONObject("pet")),
            allSessions = objectJson.optJSONArray("sessions").toList(::decodeSession),
            timeBankTransactions = objectJson.optJSONArray("timeBankTransactions").toList(::decodeTransaction),
            storePurchases = objectJson.optJSONArray("storePurchases").toList(::decodePurchase),
            scheduledBlocks = objectJson.optJSONArray("scheduledBlocks").toList(::decodeBlock),
            privacyEvents = objectJson.optJSONArray("privacyEvents").toList(::decodePrivacyEvent),
            deskAudit = decodeDeskAudit(objectJson.optJSONObject("deskAudit")),
            settings = decodeSettings(objectJson.optJSONObject("settings")),
            lastSession = objectJson.optJSONObject("lastSession")?.let(::decodeSession)
        )
    }

    private fun encodePet(value: PetProfile) = JSONObject()
        .put("name", value.name)
        .put("species", value.species)
        .put("stage", value.stage)
        .put("fullness", value.fullness)
        .put("mood", value.mood)
        .put("lastFedMs", value.lastFedMs)
        .put("equippedCosmetic", value.equippedCosmetic)

    private fun decodePet(json: JSONObject?) = PetProfile(
        name = json?.optString("name", "Kitsu") ?: "Kitsu",
        species = json?.optString("species", "kitsu") ?: "kitsu",
        stage = json?.optInt("stage", 1) ?: 1,
        fullness = json?.optInt("fullness", InitialPetFullness) ?: InitialPetFullness,
        mood = json?.optString("mood", InitialPetMood) ?: InitialPetMood,
        lastFedMs = json?.optLong("lastFedMs", 0L) ?: 0L,
        equippedCosmetic = json?.optString("equippedCosmetic", "") ?: ""
    )

    private fun encodeProfile(value: UserProfile) = JSONObject()
        .put("displayName", value.displayName)
        .put("username", value.username)

    private fun decodeProfile(json: JSONObject?) = UserProfile(
        displayName = json?.optString("displayName", "Sanjiv") ?: "Sanjiv",
        username = json?.optString("username", "sual") ?: "sual"
    )

    private fun encodeSession(value: FocusSessionSummary) = JSONObject()
        .put("id", value.id)
        .put("durationMinutes", value.durationMinutes)
        .put("focusScore", value.focusScore)
        .put("success", value.success)
        .put("pointsEarned", value.pointsEarned)
        .put("distractionCount", value.distractionCount)
        .put("startTimeMs", value.startTimeMs)
        .put("endTimeMs", value.endTimeMs)
        .put("plannedDurationMinutes", value.plannedDurationMinutes)
        .put("endedEarly", value.endedEarly)
        .put("blockedApps", JSONArray(value.blockedApps))
        .put("timeBankMinutesEarned", value.timeBankMinutesEarned)
        .put("recoverySeconds", value.recoverySeconds)

    private fun decodeSession(json: JSONObject) = FocusSessionSummary(
        id = json.optLong("id"),
        durationMinutes = json.optInt("durationMinutes"),
        focusScore = json.optInt("focusScore"),
        success = json.optBoolean("success"),
        pointsEarned = json.optInt("pointsEarned"),
        distractionCount = json.optInt("distractionCount"),
        startTimeMs = json.optLong("startTimeMs", json.optLong("id")),
        endTimeMs = json.optLong("endTimeMs", json.optLong("id")),
        plannedDurationMinutes = json.optInt("plannedDurationMinutes", json.optInt("durationMinutes")),
        endedEarly = json.optBoolean("endedEarly"),
        blockedApps = json.optJSONArray("blockedApps").strings(),
        timeBankMinutesEarned = json.optInt("timeBankMinutesEarned"),
        recoverySeconds = json.optInt("recoverySeconds")
    )

    private fun encodeTransaction(value: TimeBankTransaction) = JSONObject()
        .put("id", value.id)
        .put("type", value.type)
        .put("minutes", value.minutes)
        .put("title", value.title)
        .put("detail", value.detail)
        .put("timestampMs", value.timestampMs)
        .put("rewardApp", value.rewardApp)
        .put("expiresAtMs", value.expiresAtMs)

    private fun decodeTransaction(json: JSONObject) = TimeBankTransaction(
        json.optLong("id"),
        json.optString("type"),
        json.optInt("minutes"),
        json.optString("title"),
        json.optString("detail"),
        json.optLong("timestampMs"),
        json.optString("rewardApp"),
        json.optLong("expiresAtMs")
    )

    private fun encodePurchase(value: StorePurchase) = JSONObject()
        .put("id", value.id)
        .put("itemId", value.itemId)
        .put("itemName", value.itemName)
        .put("category", value.category)
        .put("pointsSpent", value.pointsSpent)
        .put("timestampMs", value.timestampMs)
        .put("equipped", value.equipped)

    private fun decodePurchase(json: JSONObject) = StorePurchase(
        json.optLong("id"),
        json.optString("itemId"),
        json.optString("itemName"),
        json.optString("category"),
        json.optInt("pointsSpent"),
        json.optLong("timestampMs"),
        json.optBoolean("equipped")
    )

    private fun encodeBlock(value: ScheduledFocusBlock) = JSONObject()
        .put("id", value.id)
        .put("title", value.title)
        .put("startTimeMs", value.startTimeMs)
        .put("durationMinutes", value.durationMinutes)
        .put("blockedApps", JSONArray(value.blockedApps))
        .put("reminderMinutes", value.reminderMinutes)
        .put("status", value.status)

    private fun decodeBlock(json: JSONObject) = ScheduledFocusBlock(
        json.optLong("id"),
        json.optString("title"),
        json.optLong("startTimeMs"),
        json.optInt("durationMinutes"),
        json.optJSONArray("blockedApps").strings(),
        json.optInt("reminderMinutes", 10),
        json.optString("status", "scheduled")
    )

    private fun encodePrivacyEvent(value: PrivacyEvent) = JSONObject()
        .put("id", value.id)
        .put("type", value.type)
        .put("title", value.title)
        .put("detail", value.detail)
        .put("timestampMs", value.timestampMs)

    private fun decodePrivacyEvent(json: JSONObject) = PrivacyEvent(
        json.optLong("id"),
        json.optString("type"),
        json.optString("title"),
        json.optString("detail"),
        json.optLong("timestampMs")
    )

    private fun encodeDeskAudit(value: DeskAuditSummary) = JSONObject()
        .put("score", value.score)
        .put("lightingScore", value.lightingScore)
        .put("clutterScore", value.clutterScore)
        .put("phoneRiskScore", value.phoneRiskScore)
        .put("postureScore", value.postureScore)
        .put("timestampMs", value.timestampMs)

    private fun decodeDeskAudit(json: JSONObject?) = DeskAuditSummary(
        score = json?.optInt("score", 0) ?: 0,
        lightingScore = json?.optInt("lightingScore", 0) ?: 0,
        clutterScore = json?.optInt("clutterScore", 0) ?: 0,
        phoneRiskScore = json?.optInt("phoneRiskScore", 0) ?: 0,
        postureScore = json?.optInt("postureScore", 0) ?: 0,
        timestampMs = json?.optLong("timestampMs", 0L) ?: 0L
    )

    private fun encodeSettings(value: AppSettings) = JSONObject()
        .put("notificationsEnabled", value.notificationsEnabled)
        .put("hapticsEnabled", value.hapticsEnabled)
        .put("strictBlockingEnabled", value.strictBlockingEnabled)
        .put("demoModeEnabled", value.demoModeEnabled)
        .put("theme", value.theme)

    private fun decodeSettings(json: JSONObject?) = AppSettings(
        notificationsEnabled = json?.optBoolean("notificationsEnabled", true) ?: true,
        hapticsEnabled = json?.optBoolean("hapticsEnabled", true) ?: true,
        strictBlockingEnabled = json?.optBoolean("strictBlockingEnabled", false) ?: false,
        demoModeEnabled = json?.optBoolean("demoModeEnabled", false) ?: false,
        theme = json?.optString("theme", "system") ?: "system"
    )

    private fun <T> JSONArray?.toList(mapper: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index -> optJSONObject(index)?.let(mapper) }
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }
    }
}
