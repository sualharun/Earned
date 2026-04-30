package com.focusguard.ml

import android.content.Context

/**
 * Person 2 (Rayan) owns this file.
 * Runs mediapipe_face_landmark_detector.tflite via LiteRT with NPU.
 * Computes Eye Aspect Ratio from landmark positions.
 * EAR < 0.2 = eyes closed, EAR >= 0.2 = eyes open.
 */
class EyeOpenEstimator(private val context: Context) {

    fun estimate(faceCrop: FaceCrop): Float {
        // TODO: Person 2 implements:
        // 1. Resize faceCrop.bitmap to model input
        // 2. Run LiteRT inference with NPU
        // 3. Extract eye landmarks
        // 4. Compute EAR = eye height / eye width
        return 0.3f // dummy: eyes open
    }

    fun close() {
        // TODO: Release model resources
    }
}
