package com.nota.nota_sdk.vision.common

import android.graphics.Bitmap
import androidx.annotation.NonNull
import com.nota.nota_sdk.task.Task

interface Detector<DCode, DetectionResultT> {
    fun detect( bitmap : Bitmap) : Task<DCode, DetectionResultT>
}