package com.jwhae.billboard.vision.face.detection

import android.graphics.Bitmap
import com.nota.nota_sdk.task.Task
import com.nota.nota_sdk.vision.face.Face

interface FaceDetector<DetectorT>{
    fun detect(var1 : Bitmap) : Task<FaceDetectionResult, DetectorT>
}