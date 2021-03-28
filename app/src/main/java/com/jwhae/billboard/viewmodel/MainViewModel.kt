package com.nota.nota_android_gs25.viewmodel

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class MainViewModel(val app: Application) : AndroidViewModel(app){


    // Serial Device
    val serialDevice = MutableLiveData<UsbDevice>(null)
    val isConnectedToSerialDevice = MutableLiveData<Boolean>(false)

    val displayAd = MutableLiveData<Boolean>(false)

    val isRegSuccess = MutableLiveData(false)
    val regErrorMsg = MutableLiveData<String>()

    val authScore = MutableLiveData<Double>(0.0)
    val displayAuth = MutableLiveData<Boolean>(false)
}