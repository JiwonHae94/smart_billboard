package com.nota.nota_sdk.task

import androidx.annotation.NonNull

interface OnCompleteListener<TCode, TResult> {
    fun onComplete(rslt : TCode, var1 : TResult?)
}