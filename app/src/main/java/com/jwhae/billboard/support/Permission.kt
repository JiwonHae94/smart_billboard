package com.nota.nota_android_gs25.support

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object Permission{
    fun checkAllPermission(@NonNull context : Context, @NonNull permission : Array<String>) : Boolean{
        for(p in permission){
            if(ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) return false else continue
        }
        return true
    }

    fun checkPermissionsRequireRationale(@NonNull act : Activity, @NonNull permission : Array<String>) : Boolean{
        for(p in permission){
            if(ActivityCompat.shouldShowRequestPermissionRationale(act, permission[0])) return true else continue
        }
        return false
    }
}