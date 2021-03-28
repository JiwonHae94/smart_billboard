package com.nota.nota_sdk.ai.common.process.internal

import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.NonNull

data class Detection( val location : RectF,  val score : Float,  val landmark : List<PointF>)