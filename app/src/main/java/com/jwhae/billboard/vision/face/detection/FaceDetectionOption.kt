package com.jwhae.billboard.vision.face.detection

class FaceDetectionOption private constructor(val builder : Builder) {
    class Builder{
        internal var mode : Int = 0

        internal fun setMode(mode : Int) : Builder{
            this.mode = mode
            return this
        }

        internal fun build() : FaceDetectionOption{
            return FaceDetectionOption(this)
        }
    }
}