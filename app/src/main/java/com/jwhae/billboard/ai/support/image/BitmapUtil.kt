package com.nota.nota_sdk.ai.support.image

import android.graphics.Bitmap

object BitmapUtil {
    fun Bitmap.resize(width: Int, height: Int) : Bitmap{
        return Bitmap.createScaledBitmap(this, width, height, false)
    }

}