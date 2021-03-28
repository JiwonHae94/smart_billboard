package com.nota.nota_android_gs25.camera.support

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider

object CameraUtil{

    /** Returns true if the device has an available back camera. False otherwise */
    internal fun hasBackCamera(camera : ProcessCameraProvider): Boolean {
        return camera?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    internal fun hasFrontCamera(camera : ProcessCameraProvider): Boolean {
        return camera?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }


    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = Math.max(width, height).toDouble() / Math.min(width, height)
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private const val RATIO_4_3_VALUE = 4.0 / 3.0
    private const val RATIO_16_9_VALUE = 16.0 / 9.0
}