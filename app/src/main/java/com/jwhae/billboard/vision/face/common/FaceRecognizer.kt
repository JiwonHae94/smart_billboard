package com.nota.nota_sdk.vision.face.common

import android.graphics.Bitmap
import androidx.annotation.NonNull
import com.nota.nota_sdk.task.Task
import com.nota.nota_sdk.vision.face.recognition.FaceRecognition
import com.nota.nota_sdk.vision.face.recognition.RecognitionResult

interface FaceRecognizer<RecognitionResultT> {
    fun recognize( var1 : Bitmap) : Task<RecognitionResult, RecognitionResultT>
}