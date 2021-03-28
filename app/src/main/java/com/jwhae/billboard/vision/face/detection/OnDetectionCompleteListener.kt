package com.nota.nota_sdk.vision.face.recognition

import com.jwhae.billboard.vision.face.detection.FaceDetectionResult
import com.nota.nota_sdk.task.OnCompleteListener
import com.nota.nota_sdk.vision.face.Face

interface OnDetectionCompleteListener : OnCompleteListener<FaceDetectionResult, Array<Face>>