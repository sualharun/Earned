package com.focusguard.session

import com.focusguard.ml.AttentionSignal

// Person 2: Data classes representing session state
data class SessionState(
    val isActive: Boolean = false,
    val totalDurationSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val attentionScore: Float = 100f,
    val blacklistedApps: List<String> = emptyList(),
    val distractionReason: DistractionReason = DistractionReason.None,
    val lastSignal: AttentionSignal? = null,
    val blockedPackageName: String? = null
)

enum class DistractionReason {
    None,
    FaceMissing,
    LookingAway,
    EyesClosed,
    BlockedAppOpened
}
