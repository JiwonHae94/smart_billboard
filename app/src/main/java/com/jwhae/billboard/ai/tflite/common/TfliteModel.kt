package com.nota.nota_sdk.ai.tflite.common

import android.graphics.Bitmap
import com.nota.nota_sdk.ai.common.*
import com.nota.nota_sdk.ai.support.image.ImageProcessor
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import android.content.Context
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

abstract class TfliteModel<MResult> : Model<Interpreter, MResult> {
    protected val info : ModelInfo
    protected val exeOption : TfliteExcutionOption?
    abstract val executor : TfliteExecutor

    protected val inferenceOutput : HashMap<Int, Any> = HashMap()
    protected abstract val imageProcessor : ImageProcessor<TensorImage>

    constructor(context:Context, modelInfo : ModelInfo,  modelOption : TfliteExcutionOption?) : super(context, modelInfo){
        this.info = modelInfo
        this.exeOption = modelOption

        this.setModelOutput(interpreter)

        initParameters()
    }

    protected var inputWidth : Int = interpreter.getInputTensor(0).shape().get(1)
    protected var inputHeight : Int = interpreter.getInputTensor(0).shape().get(2)

    override fun loadModel(modelName: String, isEncrypted: Boolean): Interpreter {
        return TfliteModelLoader.setOption(context, exeOption).loadModel(modelName, isEncrypted)
    }

    protected var outTensorBuffer : TensorBuffer? = null

    protected open fun setModelOutput( interpreter : Interpreter){
        val indx = 0

        this.outTensorBuffer = getTensorBuffer(indx)
        inferenceOutput.put(indx, outTensorBuffer!!.buffer.rewind())
    }

    protected fun getTensorBuffer(index : Int) : TensorBuffer {
        val shape = interpreter.getOutputTensor(index).shape()
        val dataType = interpreter.getOutputTensor(index).dataType()

        return TensorBuffer.createFixedSize(shape, dataType)
    }

    protected fun Int.toFloatBuffer() : FloatBuffer {
        return ByteBuffer.allocateDirect(this)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    override fun run(bm: Bitmap) : MResult {
        var bitmap: Bitmap = bm
        this.executor.execute(bitmap)
        return processOutput()
    }
}