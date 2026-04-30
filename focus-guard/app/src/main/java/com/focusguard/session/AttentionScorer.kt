package com.focusguard.session

import com.focusguard.ml.AttentionSignal
import kotlin.math.abs

// Person 2: Computes attention score from ML pipeline outputs
class AttentionScorer {
    fun score(
        signal: AttentionSignal,
        currentScore: Float,
        focusedIncrement: Float = FOCUSED_INCREMENT,
        distractedDecrement: Float = DISTRACTED_DECREMENT
    ): AttentionScoreResult {
        val reason = distractionReasonFor(signal)
        val nextScore = if (reason == DistractionReason.None) {
            currentScore + focusedIncrement
        } else {
            currentScore - distractedDecrement
        }.coerceIn(MIN_SCORE, MAX_SCORE)

        return AttentionScoreResult(
            attentionScore = nextScore,
            isFocused = reason == DistractionReason.None,
            distractionReason = reason
        )
    }

    fun distractionReasonFor(signal: AttentionSignal): DistractionReason {
        if (!signal.faceDetected) return DistractionReason.FaceMissing
        if (abs(signal.yaw) >= MAX_ABS_YAW || abs(signal.pitch) >= MAX_ABS_PITCH) {
            return DistractionReason.LookingAway
        }
        if (signal.eyeAspectRatio <= MIN_EYE_ASPECT_RATIO) {
            return DistractionReason.EyesClosed
        }
        return DistractionReason.None
    }

    companion object {
        const val MIN_SCORE = 0f
        const val MAX_SCORE = 100f
        const val FOCUSED_INCREMENT = 0.5f
        const val DISTRACTED_DECREMENT = 1.5f
        const val MAX_ABS_YAW = 20f
        const val MAX_ABS_PITCH = 15f
        const val MIN_EYE_ASPECT_RATIO = 0.2f
    }
}

data class AttentionScoreResult(
    val attentionScore: Float,
    val isFocused: Boolean,
    val distractionReason: DistractionReason
)
