package com.nota.nota_sdk.vision.other

import android.graphics.Bitmap
import androidx.annotation.NonNull

interface VisionBaseImple<TResult> {
    fun process(vararg var1 : Any) : TResult
}