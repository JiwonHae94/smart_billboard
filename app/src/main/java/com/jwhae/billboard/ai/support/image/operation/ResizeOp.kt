package com.nota.nota_sdk.ai.support.image.ops

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.NonNull
import com.nota.nota_sdk.ai.support.image.common.BitmapOperator
import org.opencv.core.Size

class ResizeOp( val param : Any) : BitmapOperator{
    override fun apply(var1: Bitmap): Bitmap {
        if(param is Float){
            val width: Int = var1.width
            val height: Int = var1.height
            val matrix = Matrix()
            matrix.postScale(param, param)

            return Bitmap.createBitmap(var1, 0, 0, width, height, matrix, true)
        }else if(param is Size){
            return Bitmap.createScaledBitmap(var1, param.width.toInt(), param.height.toInt(), false)
        }
        return var1
    }

}