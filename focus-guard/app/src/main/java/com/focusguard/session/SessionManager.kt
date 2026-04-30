package com.focusguard.session

import com.focusguard.ml.AttentionSignal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Person 3 (Sanjiv) owns this file.
 * Singleton managing session lifecycle. UI observes stateFlow.
 */
object SessionManager {

    private val _stateFlow = MutableStateFlow(SessionState())
    val stateFlow: StateFlow<SessionState> = _stateFlow.asStateFlow()

    private val scorer = AttentionScorer()
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startSession(durationSeconds: Int, blacklistedApps: List<String>) {
        scorer.reset()
        _stateFlow.value = SessionState(
            isActive = true,
            initialDurationSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            attentionScore = 100f,
            blacklistedApps = blacklistedApps
        )
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_stateFlow.value.remainingSeconds > 0 && _stateFlow.value.isActive) {
                delay(1000)
                _stateFlow.update { state ->
                    if (!state.isActive) return@update state
                    val penalty = if (scorer.score < 50f) 2 else 0
                    state.copy(
                        remainingSeconds = (state.remainingSeconds - 1 + penalty).coerceAtLeast(0),
                        attentionScore = scorer.score,
                        distractionCount = state.distractionCount + if (penalty > 0) 1 else 0
                    )
                }
            }
            // Session ended naturally
            _stateFlow.update { it.copy(isActive = false) }
        }
    }

    fun stopSession() {
        timerJob?.cancel()
        _stateFlow.update { it.copy(isActive = false) }
    }

    fun onAttentionSignal(signal: AttentionSignal) {
        scorer.update(signal)
        _stateFlow.update { it.copy(attentionScore = scorer.score) }
    }

    fun onBlockedAppOpened(packageName: String) {
        _stateFlow.update { it.copy(blockedPackageName = packageName) }
    }

    fun clearBlockedApp() {
        _stateFlow.update { it.copy(blockedPackageName = null) }
    }
}
