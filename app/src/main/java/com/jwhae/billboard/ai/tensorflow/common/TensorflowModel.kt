package com.nota.nota_sdk.ai.tensorflow.common

import android.graphics.Bitmap
import androidx.annotation.NonNull
import com.nota.nota_sdk.ai.common.Model
import com.nota.nota_sdk.ai.common.ModelInfo
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import android.content.Context

abstract class TensorflowModel<MResult> : Model<TensorFlowInferenceInterface, MResult> {

    constructor(context:Context, modelName: String, isEncrypted: Boolean) : this(context, ModelInfo.Builder(modelName, isEncrypted).build())

    constructor(context:Context, modelInputOption: ModelInfo) : super(context, modelInputOption)

    override fun loadModel(modelName: String, isEncrypted: Boolean): TensorFlowInferenceInterface {
        return TensorflowModelLoader.getClient(context).loadModel(modelName, isEncrypted)
    }

}
