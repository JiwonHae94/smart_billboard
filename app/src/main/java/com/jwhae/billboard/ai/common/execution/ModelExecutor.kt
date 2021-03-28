package com.nota.nota_sdk.ai.common.execution

import android.graphics.Bitmap
import androidx.annotation.NonNull
import com.nota.nota_sdk.ai.common.execution.internal.OnExeCompleteListener
import com.nota.nota_sdk.ai.common.execution.internal.OnExeFailureListener
import com.nota.nota_sdk.ai.common.execution.internal.OnExeTimeElapsedListener
import org.tensorflow.lite.Interpreter

abstract class ModelExecutor<IType> {
    protected val interpreter : IType

    constructor( builder : Builder<IType>){
        this.interpreter = builder.interpreter
    }

    private var exeFailureListener : OnExeFailureListener ?= null

    private var exeTimeElapsedListener : OnExeTimeElapsedListener?= null

    internal fun addOnExecutionTimeElapsedListener( listener : OnExeTimeElapsedListener) : ModelExecutor<IType>{
        this.exeTimeElapsedListener = listener
        return this
    }
    internal fun addOnExecutionFailureListener( listener : OnExeFailureListener) : ModelExecutor<IType>{
        this.exeFailureListener = listener
        return this
    }

    protected fun onFailure( t : Throwable){
        this.exeFailureListener?.onFailure(t)
    }

    protected fun onExeTimeElapsed( timeElapsed : Long){
        this.exeTimeElapsedListener?.onTimeElapsed(timeElapsed)
    }

    abstract fun execute( bm : Bitmap) : ModelExecutor<IType>

    abstract class Builder<IType>(val interpreter : IType){
        abstract fun build() : ModelExecutor<IType>
    }
}