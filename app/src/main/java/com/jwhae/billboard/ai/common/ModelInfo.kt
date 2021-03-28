package com.nota.nota_sdk.ai.common

import androidx.annotation.NonNull
import androidx.annotation.Nullable

class ModelInfo private constructor(  val modelName : String,
                                      val isEnrypted : Boolean) {

    class Builder( val modelName : String,  val isEncrypted : Boolean) {
        fun build() : ModelInfo{
            return ModelInfo(modelName!!, isEncrypted)
        }
    }
}