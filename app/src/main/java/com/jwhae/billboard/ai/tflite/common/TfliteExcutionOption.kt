package com.nota.nota_sdk.ai.tflite.common

import androidx.annotation.NonNull
import java.lang.StringBuilder

class TfliteExcutionOption private constructor( val delegate : Delegate,  val numThread : Int) {
    enum class Delegate{
        XNNPACK, NNAPI, CPU, GPU
    }

    override fun toString(): String {
        val option = StringBuilder("TfliteModelOption")
        option.append("delegate", delegate)
        option.append("number of thread", numThread)
        return option.toString()
    }

    class Builder {
        private var delegate : Delegate = Delegate.XNNPACK
        private var numThread : Int = 4

        fun setNumThread(numThread : Int): Builder {
            this.numThread = numThread
            return this
        }

        fun setDelegate(delegateOpt : Delegate) : Builder {
            this.delegate = delegateOpt
            return this
        }

        fun build() : TfliteExcutionOption{
            return TfliteExcutionOption(delegate, numThread)
        }
    }
}