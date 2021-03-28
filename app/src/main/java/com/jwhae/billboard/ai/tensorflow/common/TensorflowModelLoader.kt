package com.nota.nota_sdk.ai.tensorflow.common

import com.nota.nota_sdk.ai.common.ModelLoader
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import android.content.Context

class TensorflowModelLoader private constructor(context: Context) : ModelLoader<TensorFlowInferenceInterface>(context){
    override fun loadModel(modelName: String, isEncrypted: Boolean): TensorFlowInferenceInterface {
        val modelStream = loadInputStream(modelName)
        return TensorFlowInferenceInterface(modelStream)
    }

    override fun close() {}

    companion object{
        fun getClient(context:Context) : TensorflowModelLoader{
            return TensorflowModelLoader(context)
        }
    }
}