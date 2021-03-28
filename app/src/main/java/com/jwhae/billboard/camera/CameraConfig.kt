package com.jwhae.billboard.camera

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis

class CameraConfig private constructor(val builder : Builder) {
    internal val lensFacing = builder.lensFacing
    internal val imgAnalysisStrategy : Int = builder.imgAnalysisStrat
    internal val screenRatio : Int = builder.screenRatio

    class Builder(){
        internal var screenRatio : Int = AspectRatio.RATIO_16_9
        internal var lensFacing : Int = CameraSelector.LENS_FACING_BACK
        internal var imgAnalysisStrat : Int = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST

        fun setImgAnalysisStrategy(strat : Int) : Builder{
            this.imgAnalysisStrat = strat
            return this
        }

        fun setLensFacing(facing : Int) : Builder{
            lensFacing = facing
            return this
        }

        fun setScreenRatio(ratio : Int): Builder{
            this.screenRatio = ratio
            return this
        }

        fun build() : CameraConfig {
            return CameraConfig(this)
        }
    }
}
