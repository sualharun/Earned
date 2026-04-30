package com.focusguard.ml

import android.content.Context
import android.graphics.Bitmap

/**
 * Person 1 (Gabe) owns this file.
 * Runs mediapipe_face_detector.tflite via LiteRT CompiledModel API with Accelerator.NPU.
 * Input: normalized float tensor [1, 256, 256, 3]
 * Output: bounding box + confidence → FaceCrop
 */
class FaceDetector(private val context: Context) {

    fun detect(bitmap: Bitmap): FaceCrop? {
        // TODO: Person 1 implements:
        // 1. Resize bitmap to 256x256
        // 2. Normalize to float tensor
        // 3. Run LiteRT CompiledModel inference with NPU
        // 4. Parse bounding box + confidence from output
        // 5. If confidence >= 0.7, crop face region and return FaceCrop
        // 6. If confidence < 0.7, return null
        return null
    }

    fun close() {
        // TODO: Release model resources
    }
}
