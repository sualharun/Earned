package com.focusguard.session

import com.focusguard.ml.AttentionSignal
import kotlin.math.abs

/**
 * Person 3 (Sanjiv) owns this file.
 * Stub implementation so Person 4's UI can build against it now.
 */
class AttentionScorer {
    var score: Float = 100f
        private set

    fun isFocused(signal: AttentionSignal): Boolean {
        return signal.faceDetected
            && abs(signal.yaw) < 20f
            && abs(signal.pitch) < 15f
            && signal.eyeAspectRatio > 0.2f
    }

    fun update(signal: AttentionSignal) {
        if (isFocused(signal)) {
            score = (score + 0.5f).coerceAtMost(100f)
        } else {
            score = (score - 1.5f).coerceAtLeast(0f)
        }
    }

    fun reset() {
        score = 100f
    }
}
