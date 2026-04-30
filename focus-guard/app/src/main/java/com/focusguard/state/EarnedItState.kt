package com.focusguard.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

data class PetProfile(
    val name: String = "Kitsu",
    val species: String = "kitsu",
    val stage: Int = 3,
    val fullness: Int = 78,
    val mood: String = "Happy"
)

data class FocusSessionSummary(
    val id: Long,
    val durationMinutes: Int,
    val focusScore: Int,
    val success: Boolean,
    val pointsEarned: Int,
    val distractionCount: Int
)

data class EarnedItUiState(
    val onboardingComplete: Boolean = false,
    val points: Int = 2840,
    val streakDays: Int = 7,
    val timeBankMinutes: Int = 35,
    val pet: PetProfile = PetProfile(),
    val sessionsToday: List<FocusSessionSummary> = listOf(
        FocusSessionSummary(
            id = 1,
            durationMinutes = 45,
            focusScore = 91,
            success = true,
            pointsEarned = 140,
            distractionCount = 1
        ),
        FocusSessionSummary(
            id = 2,
            durationMinutes = 25,
            focusScore = 84,
            success = true,
            pointsEarned = 105,
            distractionCount = 2
        )
    ),
    val lastSession: FocusSessionSummary? = null
) {
    val focusMinutesToday: Int
        get() = sessionsToday.filter { it.success }.sumOf { it.durationMinutes }
}

object EarnedItStore {
    private val _state = MutableStateFlow(EarnedItUiState())
    val state: StateFlow<EarnedItUiState> = _state.asStateFlow()

    fun finalizeOnboarding() {
        _state.update { it.copy(onboardingComplete = true) }
    }

    fun pickPet(species: String, name: String) {
        _state.update {
            it.copy(
                pet = it.pet.copy(
                    species = species,
                    name = name.ifBlank { species.replaceFirstChar { c -> c.uppercase() } },
                    stage = 1,
                    fullness = 84,
                    mood = "Happy"
                )
            )
        }
    }

    fun recordSession(durationSeconds: Int, focusScore: Float, distractionCount: Int, endedEarly: Boolean) {
        val durationMinutes = (durationSeconds / 60f).roundToInt().coerceAtLeast(1)
        val score = focusScore.roundToInt().coerceIn(0, 100)
        val success = !endedEarly && score >= 70
        val points = if (success) {
            (durationMinutes * 3 + score / 2 + if (distractionCount == 0) 25 else 0)
        } else {
            0
        }
        _state.update { state ->
            val session = FocusSessionSummary(
                id = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                focusScore = score,
                success = success,
                pointsEarned = points,
                distractionCount = distractionCount
            )
            state.copy(
                points = state.points + points,
                timeBankMinutes = state.timeBankMinutes + if (success) durationMinutes / 5 else 0,
                pet = state.pet.copy(
                    fullness = (state.pet.fullness + if (success) 8 else -6).coerceIn(0, 100),
                    mood = when {
                        success && score >= 90 -> "Energized"
                        success -> "Happy"
                        else -> "Bored"
                    }
                ),
                sessionsToday = listOf(session) + state.sessionsToday,
                lastSession = session
            )
        }
    }
}
