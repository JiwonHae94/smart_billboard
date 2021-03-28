package com.nota.nota_sdk.vision.face

import android.graphics.PointF
import android.graphics.RectF

data class Face(val rectf : RectF, val landmarks : List<PointF>){
    internal fun getLandmarks() : DoubleArray{
        return doubleArrayOf(
            landmarks.get(0).x.toDouble(), landmarks.get(0).y.toDouble(),  // left eye
            landmarks.get(1).x.toDouble(), landmarks.get(1).y.toDouble(),  // right eye
            landmarks.get(2).x.toDouble(), landmarks.get(2).y.toDouble(),  // nose
            landmarks.get(3).x.toDouble(), landmarks.get(3).y.toDouble(),  // mouth left
            landmarks.get(4).x.toDouble(), landmarks.get(4).y.toDouble()   // mouth right
        )
    }

    override fun toString(): String {
        return "Face(loc=$rectf, landmarks=$landmarks)"
    }

    companion object{
        const val EYE = 0

    }
    

}