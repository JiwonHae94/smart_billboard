package com.nota.nota_sdk.ai.tflite.common

import android.graphics.Bitmap
import android.util.Log
import com.nota.nota_sdk.ai.common.execution.ModelExecutor
import androidx.annotation.NonNull
import com.nota.nota_sdk.ai.common.Platform
import com.nota.nota_sdk.ai.common.execution.internal.OnExeCompleteListener
import com.nota.nota_sdk.ai.common.execution.internal.OnExeFailureListener
import com.nota.nota_sdk.ai.common.execution.internal.OnExeTimeElapsedListener
import com.nota.nota_sdk.ai.support.image.ImageProcessor
import com.nota.nota_sdk.ai.support.image.common.internal.DataType
import org.tensorflow.lite.Interpreter

import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.FloatBuffer

class TfliteExecutor : ModelExecutor<Interpreter> {
    private val imageProcessor : ImageProcessor<TensorImage>
    private val inputTensorImage : TensorImage
    private val inferenceOutput : HashMap<Int, Any>

    private constructor(builder : Builder) : super(builder){
        this.imageProcessor = builder.imageProcessor!!
        this.inputTensorImage = TensorImage(interpreter!!.getInputTensor(0).dataType())
        this.inferenceOutput = builder.modelOutput
    }

    override fun execute(bm: Bitmap): ModelExecutor<Interpreter> {
        val startTime = System.currentTimeMillis()

        interpreter.runForMultipleInputsOutputs(arrayOf(imageProcessor.process(bm).buffer.rewind()), inferenceOutput)
        onExeTimeElapsed(System.currentTimeMillis() - startTime)
        return this
    }

    class Builder : ModelExecutor.Builder<Interpreter> {
        internal var imageProcessor : ImageProcessor<TensorImage>? = null
        internal var modelOutput : HashMap<Int, Any>
        internal var tfliteInterpreter : Interpreter

        constructor( interpreter: Interpreter,  tensorBuffer : TensorBuffer) : this(interpreter, tensorBuffer.buffer.rewind() as FloatBuffer)

        constructor( interpreter: Interpreter,  floatBuffer : FloatBuffer ) : this(interpreter, hashMapOf(0 to floatBuffer as Any))

        constructor( interpreter: Interpreter,  modelOutput : HashMap<Int, Any>) : super(interpreter){
            this.tfliteInterpreter = interpreter
            this.modelOutput = modelOutput
        }

        internal fun setInputProcessor( imageProcessor : ImageProcessor<TensorImage>) : Builder{
            this.imageProcessor = imageProcessor
            return this
        }

        override fun build(): TfliteExecutor {
            val processor = imageProcessor?: ImageProcessor.Builder<TensorImage>(Platform.Tflite, DataType.FLOAT32).build()
            this.imageProcessor = processor

            return TfliteExecutor(this)
        }
    }
}