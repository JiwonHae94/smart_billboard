package com.nota.nota_sdk.ai.common

import androidx.annotation.NonNull
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import java.io.InputStream
import kotlin.jvm.Throws
import android.content.Context

abstract class ModelLoader<Out>(val context:Context){
    abstract fun loadModel( modelName : String,  isEncrypted: Boolean) : Out

    @Throws(IOException::class)
    protected fun loadAssets( modelName : String,  isEncrypted : Boolean) : ByteArray {
        if(isEncrypted)
            return loadEncryptedBytes(modelName)
        else
            return loadBytes(modelName)
    }

    internal fun loadInputStream(modelName: String) : InputStream {
        return context.resources.assets.open(modelName)
    }

    private fun loadBytes( modelName : String) : ByteArray{
        val inputStream = loadInputStream(modelName)
        val data = ByteArray(inputStream.available())
        inputStream.read(data)
        return data
    }

    private fun loadEncryptedBytes( modelName : String) : ByteArray {
        val ivSpec = IvParameterSpec(Base64.decode(IV_KEY, 0))
        val keySpec = SecretKeySpec(Base64.decode(ENC_KEY, 0), "AES")
        val decInstance = Cipher.getInstance("AES/CBC/PKCS5Padding")
        decInstance.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        val stream = loadInputStream(modelName)
        val cis = CipherInputStream(stream, decInstance)
        val decArray = cis.readBytes()

        return decArray
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    abstract fun close()

    companion object{
        const val ENC_KEY = "SVeFJLeYm9vWQtuia7DVU47gyZaqOUMF1CKNfJ4JnY4="
        const val IV_KEY = "l3iV+gXouI57PGupbP0zGQ=="
    }
}