package com.nota.nota_sdk.ai.support.image.common

import com.nota.nota_sdk.ai.support.image.common.internal.DataType

object DataTypeConverter {
    fun toTfliteDataType(type : DataType) : org.tensorflow.lite.DataType{
        return when(type){
            DataType.FLOAT32 -> org.tensorflow.lite.DataType.FLOAT32
            DataType.INT8    -> org.tensorflow.lite.DataType.INT8
            DataType.UINT8 -> org.tensorflow.lite.DataType.UINT8
        }
    }
}