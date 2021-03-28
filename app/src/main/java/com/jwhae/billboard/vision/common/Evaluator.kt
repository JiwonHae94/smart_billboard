package com.nota.nota_sdk.vision.common

import android.graphics.Bitmap
import androidx.annotation.NonNull
import com.nota.nota_sdk.task.Task

interface Evaluator<ECode, EvaluationResultT> {
    fun evaluate( bm : Bitmap) : Task<ECode, EvaluationResultT>
}