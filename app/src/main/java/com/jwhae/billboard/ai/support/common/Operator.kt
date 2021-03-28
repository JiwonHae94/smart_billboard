package com.nota.nota_sdk.ai.support.common

import androidx.annotation.NonNull

interface Operator<T> {
    fun apply( var1 : T): T
}