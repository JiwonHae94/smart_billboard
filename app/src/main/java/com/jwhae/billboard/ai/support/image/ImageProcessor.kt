package com.nota.nota_sdk.ai.support.image

import android.graphics.Bitmap
import com.nota.nota_sdk.ai.common.Platform
import com.nota.nota_sdk.ai.support.image.common.BitmapOperator
import com.nota.nota_sdk.ai.support.image.common.DataTypeConverter
import com.nota.nota_sdk.ai.support.image.common.internal.DataType
import com.nota.nota_sdk.ai.support.image.ops.AlignOp
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.image.ImageOperator
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.TensorOperatorWrapper
import java.nio.ByteOrder
import java.util.*

class ImageProcessor<T> {
    private val bitmapOperatorList : ArrayList<BitmapOperator>
    private val tfliteOperatorList : ArrayList<ImageOperator>
    private val platform : Platform
    private val modelDataType : DataType

    private constructor(builder : Builder<T>){
        this.bitmapOperatorList = builder.bitmapOps
        this.tfliteOperatorList = builder.tfliteOps
        this.platform = builder.platform
        this.modelDataType = builder.modelDataType
    }

    var count = 0

    internal fun process( bm : Bitmap) : T{
        var bitmap : Bitmap = bm

        bitmapOperatorList.iterator().forEach {
            bitmap = it.apply(bitmap)
        }

        return when(platform){
            Platform.Tflite-> {
                val tensorImage = TensorImage(DataTypeConverter.toTfliteDataType(modelDataType))
                tensorImage.load(bitmap)
                tensorImage.buffer.order(ByteOrder.nativeOrder())

                processTfliteOperation(tensorImage) as T
            }
            else -> bitmap as T
        }
    }

    private fun processTfliteOperation(image : TensorImage) : TensorImage {
        var x = image

        this.tfliteOperatorList.iterator().forEach {
            x = it.apply(x)
        }
        return x
    }


    class Builder<T>{
        internal val platform : Platform

        internal val bitmapOps = ArrayList<BitmapOperator>()

        internal val tfliteOps = ArrayList<ImageOperator>()

        internal var modelDataType : DataType

        constructor( platform : Platform, dataType : DataType){
            this.platform = platform
            modelDataType = dataType
        }

        fun add(op : BitmapOperator) : Builder<T> {
            bitmapOps.add(op)
            return this
        }

        fun add(op: ImageOperator): Builder<T> {
            tfliteOps.add(op)
            return this
        }

        fun add(op: TensorOperator): Builder<T> {
            return add(TensorOperatorWrapper(op) as ImageOperator)
        }

        fun build() : ImageProcessor<T> {
            return ImageProcessor(this)
        }
    }
}