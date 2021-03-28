package com.nota.nota_sdk.vision.face

import androidx.annotation.NonNull

data class Detection( val face : Face,  val score : Float){
    override fun toString(): String {
        val stringBuilder = StringBuilder("FaceDetection")
        stringBuilder.append("face detected", face)
        stringBuilder.append("detection score", score)
        return stringBuilder.toString()
    }
}