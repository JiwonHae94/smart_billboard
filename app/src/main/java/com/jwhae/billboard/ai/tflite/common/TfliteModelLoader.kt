package com.nota.nota_sdk.ai.tflite.common

import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import android.content.Context
import com.nota.nota_sdk.ai.common.ModelLoader
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.io.PrintStream
import java.lang.RuntimeException
import java.nio.ByteBuffer
import kotlin.jvm.Throws

class TfliteModelLoader private constructor(context:Context, option : TfliteExcutionOption?) : ModelLoader<Interpreter>(context) {
    private val tfliteOption : Interpreter.Options
    //private var gpuDelegate : GpuDelegate? =null

    init{
        val compatList = CompatibilityList()
        if (compatList.isDelegateSupportedOnThisDevice) {
            val delegateOptions = compatList.bestOptionsForThisDevice
            //gpuDelegate = GpuDelegate(delegateOptions)
        }

        val opt = convertOption(option)?:
            Interpreter.Options().apply {
                if (compatList.isDelegateSupportedOnThisDevice) {
                    // if the device has a supported GPU, add the GPU delegate
                    /*gpuDelegate?.let{
                        this.addDelegate(gpuDelegate)
                    }*/

                } else {
                    // if the GPU is not supported, run on 4 threads
                    this.setNumThreads(4)
                }
            }
        tfliteOption = opt
    }


    @Throws(RuntimeException::class)
    private fun convertOption(option : TfliteExcutionOption?) : Interpreter.Options? {
        if(option == null)
            return null

        val interpreterOpt = Interpreter.Options()
        interpreterOpt.setNumThreads(option.numThread)
        Log.d(TfliteModelLoader::class.java.simpleName, "${option.delegate}")

        when(option.delegate){
            TfliteExcutionOption.Delegate.XNNPACK-> interpreterOpt.setUseXNNPACK(true)
            TfliteExcutionOption.Delegate.NNAPI -> interpreterOpt.setUseNNAPI(true)
            TfliteExcutionOption.Delegate.GPU -> {
                /*gpuDelegate?.let{
                    interpreterOpt.addDelegate(gpuDelegate)
                }*/
            }
            TfliteExcutionOption.Delegate.CPU ->{
                Log.d(TfliteModelLoader::class.java.simpleName, "CPU delegate")
            }
            else ->{}
        }
        return interpreterOpt
    }

    override fun loadModel(modelName: String, isEncrypted: Boolean): Interpreter {
        if(isEncrypted){
            val bytes = loadAssets(modelName, true)
            val byteBuffer = ByteBuffer.allocateDirect(bytes.size)
            byteBuffer.put(bytes)
            return Interpreter(byteBuffer, tfliteOption)
        }
        return Interpreter(FileUtil.loadMappedFile(context, modelName), tfliteOption)
    }

    override fun close() {
        //gpuDelegate?.close()
    }

    companion object{
        fun setOption(context:Context, option : TfliteExcutionOption?): TfliteModelLoader{
            return TfliteModelLoader(context, option)
        }
    }
}