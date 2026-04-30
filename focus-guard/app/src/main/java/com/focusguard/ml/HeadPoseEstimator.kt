package com.focusguard.ml

// Person 1: Head pose estimation using WHENet TFLite model
class HeadPoseEstimator {
    fun estimate(faceCrop: FaceCrop): HeadPose {
        // Person 1/2 replaces this with WHENet LiteRT inference.
        return HeadPose(yaw = 0f, pitch = 0f, roll = 0f)
    }
}
