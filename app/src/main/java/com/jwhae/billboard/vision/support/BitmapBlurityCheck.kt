package com.nota.nota_sdk.vision.support

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import android.graphics.RectF
import android.util.Log

object BitmapBlurityCheck {
    @Synchronized
    fun process(var1 : Bitmap, rect : RectF) : Double {
        try{
            val croppedBmp = Bitmap.createBitmap(var1, rect.left.toInt(), rect.top.toInt(), rect.width().toInt(), rect.height().toInt())
            return laplacian(croppedBmp)
        }catch(e: Exception){
            Log.e(BitmapBlurityCheck::class.java.simpleName, "$e")
        }

        return Double.MIN_VALUE
    }

    private fun ftt(var1 : Bitmap) : Double{
        val srcImg = Mat()
        Utils.bitmapToMat(var1, srcImg)
        Imgproc.cvtColor(srcImg, srcImg, Imgproc.COLOR_BGR2GRAY)

        val padded = Mat() //expand input image to optimal size

        val m = Core.getOptimalDFTSize(srcImg.rows())
        val n = Core.getOptimalDFTSize(srcImg.cols()) // on the border add zero values

        Core.copyMakeBorder(srcImg, padded, 0, m - srcImg.rows(), 0, n - srcImg.cols(), Core.BORDER_CONSTANT, Scalar.all(0.0))

        val planes: MutableList<Mat> = ArrayList()
        padded.convertTo(padded, CvType.CV_32F)
        planes.add(padded)
        planes.add(Mat.zeros(padded.size(), CvType.CV_32F))
        val complexI = Mat()
        Core.merge(planes, complexI) // Add to the expanded another plane with zeros

        Core.dft(complexI, complexI)

        // compute the magnitude and switch to logarithmic scale
        // => log(1 + sqrt(Re(DFT(I))^2 + Im(DFT(I))^2))

        // compute the magnitude and switch to logarithmic scale
        // => log(1 + sqrt(Re(DFT(I))^2 + Im(DFT(I))^2))
        Core.split(complexI, planes) // planes.get(0) = Re(DFT(I)

        // planes.get(1) = Im(DFT(I))
        // planes.get(1) = Im(DFT(I))
        Core.magnitude(planes[0], planes[1], planes[0]) // planes.get(0) = magnitude

        var magI = planes[0]
        val matOfOnes = Mat.ones(magI.size(), magI.type())
        Core.add(matOfOnes, magI, magI) // switch to logarithmic scale

        Core.log(magI, magI)
        // crop the spectrum, if it has an odd number of rows or columns

        magI = magI.submat(Rect(0, 0, magI.cols() and -2, magI.rows() and -2))
        // rearrange the quadrants of Fourier image  so that the origin is at the image center

        val cx = magI.cols() / 2
        val cy = magI.rows() / 2
        val q0 = Mat(magI, Rect(0, 0, cx, cy)) // Top-Left - Create a ROI per quadrant
        val q1 = Mat(magI, Rect(cx, 0, cx, cy)) // Top-Right
        val q2 = Mat(magI, Rect(0, cy, cx, cy)) // Bottom-Left
        val q3 = Mat(magI, Rect(cx, cy, cx, cy)) // Bottom-Right
        val tmp = Mat() // swap quadrants (Top-Left with Bottom-Right)

        q0.copyTo(tmp)
        q3.copyTo(q0)
        tmp.copyTo(q3)
        q1.copyTo(tmp) // swap quadrant (Top-Right with Bottom-Left)

        q2.copyTo(q1)
        tmp.copyTo(q2)
        magI.convertTo(magI, CvType.CV_8UC1)
        Core.normalize(magI, magI, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1) // Transform the matrix with float values

        // into a viewable image form (float between
        // values 0 and 255).

        val mu = MatOfDouble()
        val sigma = MatOfDouble()
        Core.meanStdDev(magI, mu, sigma)

        return mu.get(0,0)[0]
    }

    private fun laplacian(var1 : Bitmap) : Double {
        val srcimg = Mat()
        Utils.bitmapToMat(var1, srcimg)

        Imgproc.cvtColor(srcimg, srcimg, Imgproc.COLOR_BGR2GRAY)
        val lap = Mat()
        Imgproc.Laplacian(srcimg, lap, CvType.CV_64F)
        return getVariance(lap)
    }

    private fun getVariance(mat : Mat) : Double {
        val median = MatOfDouble()
        val std = MatOfDouble()
        Core.meanStdDev(mat, median, std)

        return Math.pow(std.get(0, 0)[0], 2.0)
    }
}