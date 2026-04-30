package com.focusguard.session

import com.focusguard.ml.AttentionSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Person 2: Manages focus session lifecycle
object SessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scorer = AttentionScorer()
    private val _state = MutableStateFlow(SessionState())
    private var countdownJob: Job? = null
    private var distractedElapsedSeconds = 0f

    val stateFlow: StateFlow<SessionState> = _state.asStateFlow()
    val currentState: SessionState
        get() = _state.value

    fun startSession(durationSeconds: Int, blacklistedApps: List<String>) {
        countdownJob?.cancel()
        distractedElapsedSeconds = 0f

        _state.value = SessionState(
            isActive = true,
            totalDurationSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            blacklistedApps = blacklistedApps.distinct(),
            attentionScore = AttentionScorer.MAX_SCORE
        )

        countdownJob = scope.launch {
            while (_state.value.isActive && _state.value.remainingSeconds > 0) {
                delay(ONE_SECOND_MS)
                _state.update { state ->
                    state.copy(remainingSeconds = (state.remainingSeconds - 1).coerceAtLeast(0))
                }
            }

            if (_state.value.remainingSeconds <= 0) {
                stopSession()
            }
        }
    }

    fun stopSession() {
        countdownJob?.cancel()
        countdownJob = null
        distractedElapsedSeconds = 0f
        _state.update { it.copy(isActive = false, blockedPackageName = null) }
    }

    fun updateBlacklistedApps(packageNames: List<String>) {
        _state.update { it.copy(blacklistedApps = packageNames.distinct()) }
    }

    fun addBlacklistedApp(packageName: String) {
        _state.update { state ->
            state.copy(blacklistedApps = (state.blacklistedApps + packageName).distinct())
        }
    }

    fun removeBlacklistedApp(packageName: String) {
        _state.update { state ->
            state.copy(blacklistedApps = state.blacklistedApps.filterNot { it == packageName })
        }
    }

    fun onAttentionSignal(signal: AttentionSignal, frameDeltaSeconds: Float = DEFAULT_FRAME_DELTA_SECONDS) {
        val state = _state.value
        if (!state.isActive) return

        val result = scorer.score(signal, state.attentionScore)
        distractedElapsedSeconds = if (result.isFocused) {
            0f
        } else {
            distractedElapsedSeconds + frameDeltaSeconds
        }

        val penaltySeconds = if (distractedElapsedSeconds >= 1f) {
            distractedElapsedSeconds -= 1f
            DISTRACTION_TIME_PENALTY_SECONDS
        } else {
            0
        }

        _state.update {
            it.copy(
                remainingSeconds = it.remainingSeconds + penaltySeconds,
                attentionScore = result.attentionScore,
                distractionReason = result.distractionReason,
                lastSignal = signal
            )
        }
    }

    fun onBlockedAppOpened(packageName: String) {
        _state.update {
            it.copy(
                distractionReason = DistractionReason.BlockedAppOpened,
                blockedPackageName = packageName
            )
        }
    }

    fun clearBlockedApp() {
        _state.update { it.copy(blockedPackageName = null) }
    }

    fun isPackageBlocked(packageName: String): Boolean {
        return _state.value.isActive &&
            packageName != EMERGENCY_DIALER_PACKAGE &&
            packageName in _state.value.blacklistedApps
    }

    private const val ONE_SECOND_MS = 1_000L
    private const val DEFAULT_FRAME_DELTA_SECONDS = 1f / 15f
    private const val DISTRACTION_TIME_PENALTY_SECONDS = 2
    const val EMERGENCY_DIALER_PACKAGE = "com.android.dialer"
}
