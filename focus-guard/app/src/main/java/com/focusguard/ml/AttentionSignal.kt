package com.focusguard.ml

data class AttentionSignal(
    val faceDetected: Boolean,
    val yaw: Float,            // degrees, negative=left, positive=right
    val pitch: Float,          // degrees, negative=down, positive=up
    val roll: Float,           // degrees, head tilt
    val eyeAspectRatio: Float  // 0.0 (closed) to ~0.4 (wide open)
)
