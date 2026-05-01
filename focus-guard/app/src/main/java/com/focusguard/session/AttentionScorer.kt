package com.focusguard.session

import kotlin.math.abs

// Person 2: Computes attention score from ML pipeline outputs
class AttentionScorer(
    private val config: AttentionScoringConfig = AttentionScoringConfig()
) {
    fun score(
        input: AttentionInput,
        currentScore: Float,
    ): AttentionScoreResult {
        val reason = distractionReasonFor(input)
        val nextScore = if (reason == DistractionReason.None) {
            currentScore + config.recoveryRate
        } else {
            currentScore - config.decayRate
        }.coerceIn(config.minScore, config.maxScore)

        return AttentionScoreResult(
            attentionScore = nextScore,
            isFocused = reason == DistractionReason.None && nextScore >= config.focusedThreshold,
            distractionReason = reason
        )
    }

    fun distractionReasonFor(input: AttentionInput): DistractionReason {
        if (!input.faceDetected) return DistractionReason.FaceMissing
        if (input.faceConfidence < config.minFaceConfidence || input.eyeConfidence < config.minEyeConfidence) {
            return DistractionReason.LowConfidence
        }
        if (abs(input.yaw) > config.maxAbsYaw || abs(input.pitch) > config.maxAbsPitch) {
            return DistractionReason.LookingAway
        }
        if (input.eyeAspectRatio <= config.minEyeAspectRatio) {
            return DistractionReason.EyesClosed
        }
        return DistractionReason.None
    }

    companion object {
        const val MIN_SCORE = 0f
        const val MAX_SCORE = 1f
    }
}

data class AttentionScoringConfig(
    val minScore: Float = AttentionScorer.MIN_SCORE,
    val maxScore: Float = AttentionScorer.MAX_SCORE,
    val focusedThreshold: Float = 0.65f,
    val recoveryRate: Float = 0.075f,
    val decayRate: Float = 0.075f,
    val maxAbsYaw: Float = 20f,
    val maxAbsPitch: Float = 15f,
    val minEyeAspectRatio: Float = 0.2f,
    val minFaceConfidence: Float = 0.5f,
    val minEyeConfidence: Float = 0.5f
)

data class AttentionScoreResult(
    val attentionScore: Float,
    val isFocused: Boolean,
    val distractionReason: DistractionReason
)
