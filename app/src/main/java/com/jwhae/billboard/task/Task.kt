package com.nota.nota_sdk.task

/**
 * Task refers to a pipeline that collectively execute processes
 *
 * @param TCode refers data type that indicates the state of the current task
 * @param TResult is the data type of the outcome produced upon task completion
 *
 */
abstract class Task<TCode, TResult> {
    protected var onTaskCompleListener : OnCompleteListener<TCode, TResult>? = null

    abstract fun onComplete(var1 : TResult?)

    abstract fun addOnCompleteListener( listener : OnCompleteListener<TCode, TResult>) : Task<TCode, TResult>
}