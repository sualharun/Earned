package com.focusguard.ml

// Person 1: Eye openness estimation using eye landmark TFLite model
class EyeOpenEstimator {
    fun estimate(faceCrop: FaceCrop): EyeState {
        // Person 1/2 replaces this with eye landmark LiteRT inference.
        return EyeState(eyeAspectRatio = 0.3f)
    }
}
