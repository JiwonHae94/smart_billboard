package com.nota.nota_sdk.ai.common

import android.graphics.Bitmap
import androidx.annotation.NonNull
import java.lang.RuntimeException
import android.content.Context

abstract class Model<Intepreter, MResult>{
    protected val interpreter : Intepreter
    protected val modelInfo   : ModelInfo
    protected val context     : Context

    constructor(context:Context, modelInfo : ModelInfo){
        this.context=  context
        interpreter = loadModel(modelInfo.modelName, modelInfo.isEnrypted)
        this.modelInfo = modelInfo

        if(interpreter == null){
            throw RuntimeException("model must be set check model name and encryption")
        }

    }

    abstract internal fun run( bm : Bitmap) : MResult

    abstract protected fun loadModel( modelName : String,  isEncrypted : Boolean) : Intepreter

    abstract protected fun initParameters()

    abstract protected fun processOutput() : MResult
}