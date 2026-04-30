package com.focusguard.ml

/**
 * Camera → FaceDetector → HeadPose/EyeOpen → AttentionSignal.
 * Person 1 (Gabe) implements start/stop with CameraX.
 * Person 2 (Rayan) wires HeadPoseEstimator + EyeOpenEstimator.
 *
 * For now, the UI uses a dummy emitter in SessionManager.
 */
class MLPipeline {
    var onAttentionSignal: ((AttentionSignal) -> Unit)? = null

    fun start() {
        // Person 1 implements: CameraX + FaceDetector → FaceCrop → Person 2 models → signal
    }

    fun stop() {
        // Person 1 implements: release camera + model resources
    }
}
