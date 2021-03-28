package com.nota.nota_android_gs25.support

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ImageSaver {
    @Synchronized
    fun saveBitmap(context: Context, bitmap : Bitmap, folderName : String, fileName : String){
        val folder = File(context.getExternalFilesDir(null), folderName)

        if(!folder.exists()) {
            folder.mkdirs()
        }

        var count =0
        var file = File(folder, "${fileName}_$count.png")

        while(file.exists()){
            count += 1
            file = File(folder, "${fileName}_$count.png")
        }

        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()

        } catch (e: Exception) {
            Log.e(ImageSaver::class.java.simpleName, "$e")
        }
    }
}
