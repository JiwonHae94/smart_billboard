package com.nota.nota_sdk.vision.face.internal

import android.graphics.Bitmap

interface FeatureExtractionImpl {
    fun extract( var1 : Bitmap,  landmark : DoubleArray) : FloatArray
}