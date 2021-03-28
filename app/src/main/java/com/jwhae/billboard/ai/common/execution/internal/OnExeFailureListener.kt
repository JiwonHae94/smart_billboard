package com.nota.nota_sdk.ai.common.execution.internal

import androidx.annotation.NonNull

interface OnExeFailureListener {
    fun onFailure( t : Throwable)
}