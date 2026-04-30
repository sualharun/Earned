package com.focusguard.ml

import android.graphics.Bitmap

data class FaceCrop(
    val bitmap: Bitmap,
    val confidence: Float
)
