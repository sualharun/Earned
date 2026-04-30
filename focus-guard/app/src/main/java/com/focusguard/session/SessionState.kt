package com.focusguard.session

// Person 2: Data classes representing session state
data class SessionState(
    val isActive: Boolean = false,
    val attentionScore: Float = 0f,
    val sessionDurationMs: Long = 0
)
