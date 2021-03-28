package com.jwhae.billboard.vision.face.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.jwhae.billboard.ai.tflite.face.BlazeFace
import com.nota.nota_sdk.ai.tensorflow.face.mtcnn.MTCNN
import com.nota.nota_sdk.task.OnCompleteListener
import com.nota.nota_sdk.task.Task
import com.nota.nota_sdk.vision.face.Face
import com.nota.nota_sdk.vision.face.internal.FaceDetectorImpl

class FaceDetectionTask  : Task<FaceDetectionResult, Array<Face>>, FaceDetector<Array<Face>>{
    private val context : Context
    private val faceDetector : FaceDetectorImpl

    private constructor(context : Context, detectionOption : FaceDetectionOption) : super(){
        this.context = context
        faceDetector = when(detectionOption.builder.mode){
            0 -> MTCNN(context)
            else -> BlazeFace(context)
        }
    }

    override fun detect(var1: Bitmap): Task<FaceDetectionResult, Array<Face>> {
        val detectionResult = faceDetector.detect(var1)
        onComplete(detectionResult)
        return this
    }

    override fun onComplete(var1: Array<Face>?) {
        val taskResult = if(var1.isNullOrEmpty()) FaceDetectionResult.NO_FACE else FaceDetectionResult.FACE_DETECTED
        this.onTaskCompleListener?.onComplete(taskResult, var1)
    }

    override fun addOnCompleteListener(listener: OnCompleteListener<FaceDetectionResult, Array<Face>>): Task<FaceDetectionResult, Array<Face>> {
        this.onTaskCompleListener = listener
        return this
    }

    companion object{
        fun newInstance(context:Context, option : FaceDetectionOption) : FaceDetectionTask{
            return FaceDetectionTask(context, option)
        }
    }
}