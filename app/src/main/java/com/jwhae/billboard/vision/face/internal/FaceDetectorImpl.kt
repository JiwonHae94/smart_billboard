package com.nota.nota_sdk.vision.face.internal

import android.graphics.Bitmap
import androidx.annotation.NonNull
import com.nota.nota_sdk.vision.face.Face

interface FaceDetectorImpl{
    fun detect( var1 : Bitmap) : Array<Face>
}