package com.focusguard.session

data class SessionState(
    val isActive: Boolean = false,
    val initialDurationSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val attentionScore: Float = 100f,
    val blacklistedApps: List<String> = emptyList(),
    val distractionCount: Int = 0,
    val blockedPackageName: String? = null
)
