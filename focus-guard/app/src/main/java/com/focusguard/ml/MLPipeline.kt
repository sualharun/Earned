package com.focusguard.ml

import android.content.Context
import android.graphics.Bitmap

// Person 1: Orchestrates face detection, head pose, and eye openness models
class MLPipeline(context: Context) {
    private val faceDetector = FaceDetector(context)
    private val headPoseEstimator = HeadPoseEstimator(context)
    private val eyeOpenEstimator = EyeOpenEstimator()

    var onAttentionSignal: ((AttentionSignal) -> Unit)? = null
    var isRunning: Boolean = false
        private set

    fun start() {
        isRunning = true
    }

    fun stop() {
        isRunning = false
    }

    suspend fun processFrame(frameBitmap: Bitmap): AttentionSignal {
        val signal = when (val result = faceDetector.detect(frameBitmap)) {
            is FaceDetectionResult.Detected -> {
                val pose = headPoseEstimator.estimate(result.faceCrop)
                val eyes = eyeOpenEstimator.estimate(result.faceCrop)

                AttentionSignal(
                    faceDetected = result.faceCrop.confidence >= FACE_CONFIDENCE_THRESHOLD,
                    yaw = pose.yaw,
                    pitch = pose.pitch,
                    roll = pose.roll,
                    eyeAspectRatio = eyes.eyeAspectRatio
                )
            }

            FaceDetectionResult.NoFace -> AttentionSignal(faceDetected = false)
        }

        onAttentionSignal?.invoke(signal)
        return signal
    }

    fun submitAttentionSignal(signal: AttentionSignal) {
        onAttentionSignal?.invoke(signal)
    }

    companion object {
        const val FACE_CONFIDENCE_THRESHOLD = 0.7f
    }
}
