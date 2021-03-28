package com.nota.nota_android_gs25.camera.analyzer

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.nota.nota_android_gs25.camera.support.ImageProxyUtil.getBitmap
import com.nota.nota_sdk.vision.face.recognition.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import android.content.Context
import com.jwhae.billboard.vision.face.detection.FaceDetectionOption
import com.jwhae.billboard.vision.face.detection.FaceDetectionResult
import com.jwhae.billboard.vision.face.detection.FaceDetectionTask
import com.nota.nota_android_gs25.camera.analyzer.listener.OnAnalysisResultListener
import com.nota.nota_sdk.vision.face.Face

class FaceRecognitionAnalyzer(val context:Context, val mode : FaceAuthMode = FaceAuthMode.NORMAL) : ImageAnalysis.Analyzer, OnDetectionCompleteListener {
    private val TAG = FaceRecognitionAnalyzer::class.java.simpleName

    internal val processBitmap = AtomicBoolean(true)
    private val faceDetectionTask : FaceDetectionTask

    private var onAnalysisResult : OnAnalysisResultListener? = null

    init{
        faceDetectionTask = FaceDetectionTask.newInstance(context,
            FaceDetectionOption.Builder().build())
        faceDetectionTask.addOnCompleteListener(this)
    }

    internal fun addOnAnlaysisResultListener(listener : OnAnalysisResultListener) : FaceRecognitionAnalyzer{
        onAnalysisResult = listener
        return this
    }

    override fun analyze(image: ImageProxy) {
        image.use{
            it.getBitmap()?.let{
                if(processBitmap.get())
                    try{
                        faceDetectionTask.detect(it)
                    }catch(e : Exception){
                        Log.e(FaceRecognitionAnalyzer::class.java.simpleName, e.stackTraceToString())
                    }

            } ?: Log.d(TAG, "bitmap is null")
        }
    }

    private fun walkThrowTimer() : Timer {

        processBitmap.set(false)

        val timer = Timer()
        timer.schedule(object : TimerTask(){
            override fun run() {
                cancel()
            }

            override fun cancel(): Boolean {
                processBitmap.set(true)
                return super.cancel()
            }
        }, RECOGNITION_TIMER, RECOGNITION_TIMER)
        return timer
    }

    override fun onComplete(rslt: FaceDetectionResult, var1: Array<Face>?) {
        when(rslt){
            FaceDetectionResult.FACE_DETECTED ->{
                if(processBitmap.get()){
                    onAnalysisResult?.onAnalysisResult(AnalysisResult.FACE, var1!!)

                    if(mode == FaceAuthMode.WALK_THROUGH){
                        walkThrowTimer()
                    } else{}

                }else{
                    Log.d(TAG, "timer is already on")
                }

            }
            FaceDetectionResult.NO_FACE -> {}
        }
    }

    companion object {
        const val RECOGNITION_TIMER = 2000L
    }
}