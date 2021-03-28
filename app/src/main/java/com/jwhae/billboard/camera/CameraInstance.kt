package com.jwhae.billboard.camera


import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.lang.IllegalStateException
import java.util.concurrent.Executor
import kotlin.jvm.Throws

class CameraInstance {
    private val TAG =  CameraInstance::class.java.simpleName
    private val builder : Factory
    private var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>? = null
    private var cameraProvider : ProcessCameraProvider? = null
    private var camera : Camera? = null

    private constructor(builder : Factory){
        this.builder = builder
        cameraProviderFuture = ProcessCameraProvider.getInstance(builder.context)
    }

    internal fun getCameraProvider() : ProcessCameraProvider?{
        return cameraProvider
    }

    @Throws(IllegalStateException::class)
    internal fun setUpCamera(executor: Executor, previewView : PreviewView, imageAnalysis: ImageAnalysis.Analyzer, imageCapture: ImageCapture?, config : CameraConfig){
        Log.d(TAG, "set up camera")
        cameraProviderFuture?.let{
            it.addListener(Runnable {
                cameraProvider = it.get()
                bindCameraUsage(executor, previewView, imageAnalysis, imageCapture, config)

            }, ContextCompat.getMainExecutor(builder.context))
        }?: throw IllegalStateException("camera is not avaialble")
    }

    @Throws(IllegalStateException::class)
    private fun bindCameraUsage(executor: Executor, previewView: PreviewView, imageAnalyzer: ImageAnalysis.Analyzer, imageCapture: ImageCapture?, config : CameraConfig){
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        Log.d(TAG, "bindCameraUsage")

        // CameraSelector
        try{
            val cameraSelector = CameraSelector.Builder().requireLensFacing(config.lensFacing).build()

            val rotation = previewView.display.rotation

            val preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(config.screenRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

            val imageCapture = imageCapture ?: ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(config.screenRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetRotation(config.screenRatio)
                .setBackpressureStrategy(config.imgAnalysisStrategy)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setAnalyzer(executor, imageAnalyzer)
                }

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            try {
                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = cameraProvider.bindToLifecycle(
                    builder.context as LifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis)

                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(previewView.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }catch(e: Exception){
            Log.e(TAG, e.stackTraceToString())
        }
    }

    /** Returns true if the device has an available back camera. False otherwise */
    internal val hasFrontCamera : Boolean ?= null

    /** Returns true if the device has an available back camera. False otherwise */
    internal val hasBackCamera : Boolean by lazy {
        cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }


    class Factory(val context:Context){
        internal fun build() : CameraInstance{
            return CameraInstance(this)
        }
    }

}