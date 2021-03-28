package com.nota.nota_sdk.ai.support.image.ops

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.NonNull
import com.nota.nota_sdk.ai.support.image.BitmapUtil.resize
import com.nota.nota_sdk.ai.support.image.common.BitmapOperator
import com.nota.nota_sdk.ai.support.image.common.internal.AlignType
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.PrintStream
import java.nio.DoubleBuffer


class AlignOp(val alignType: AlignType, val landmarks: DoubleBuffer, val targetSize: Size) : BitmapOperator {
    private val dstPoints3Points = arrayOf(
        Point(32.29459953, 30.69630051),
        Point(78.53179932, 30.50139999),
        Point((36.54930115 + 74.22990036) / 2, (83.3655014 + 83.20410156) / 2)
    )

    private val resizeOp: ResizeOp = ResizeOp(targetSize)

    @Synchronized
    override fun apply(var1: Bitmap): Bitmap {
        return when (alignType) {
            AlignType.THREE_POINT, AlignType.MASK_POINT -> threePointAlign(var1)
            AlignType.FOUR_POINT -> fourPointAlign(var1)
            AlignType.FIVE_POINT -> fivePointAlign(var1)
        }
    }

    private fun threePointAlign(bm: Bitmap): Bitmap {
        if(landmarks.array().isEmpty())
            return bm.resize(targetSize.width.toInt(), targetSize.height.toInt())

        val srcimg = Mat(bm.width, bm.height, CvType.CV_8UC4)
        Utils.bitmapToMat(bm, srcimg)

        val srcPoints = arrayOfNulls<Point>(3)
        srcPoints[0] = Point(landmarks[0], landmarks[1])
        srcPoints[1] = Point(landmarks[2], landmarks[3])
        srcPoints[2] =
            Point(((landmarks[6] + landmarks[8]) / 2), ((landmarks[7] + landmarks[9]) / 2))

        val matPt_src = MatOfPoint2f()
        val matPt_dst = MatOfPoint2f()
        matPt_src.fromArray(*srcPoints) /*from  ww w  .jav a  2s.c  o m*/
        matPt_dst.fromArray(*dstPoints3Points)

        val tfm = Imgproc.getAffineTransform(matPt_src, matPt_dst)
        val srcZeroMat = Mat.zeros(bm.width, bm.height, srcimg.type())
        Imgproc.warpAffine(srcimg, srcZeroMat, tfm, targetSize)

        val result = Bitmap.createBitmap(
            srcZeroMat.width(),
            srcZeroMat.height(),
            Bitmap.Config.ARGB_8888
        )

        Utils.matToBitmap(srcZeroMat, result)
        return resizeOp.apply(result)
    }

    @Synchronized
    private fun maskPointAlign(bm: Bitmap, landmarks: DoubleArray, targetSize: Size): Bitmap {
        if(landmarks.size < 1)
            return bm.resize(targetSize.width.toInt(), targetSize.height.toInt())

        val left_eye = doubleArrayOf(landmarks.get(0), landmarks.get(1))
        val right_eye = doubleArrayOf(landmarks.get(2), landmarks.get(3))
        val mouse_left = doubleArrayOf(landmarks.get(6), landmarks.get(7))
        val mouse_right = doubleArrayOf(landmarks.get(8), landmarks.get(9))
        val middle_mouse = doubleArrayOf(
            (mouse_left[0] + mouse_right[0]) / 2,
            (mouse_left[1] + mouse_right[1]) / 2
        )

        // float margin[] = {0.5f, 0.57f, 0.57f};
        val margin = floatArrayOf(0.1f, 0.1f, 0.1f)

        // Get Width, Height
        val width =
            Math.sqrt((right_eye[0] - left_eye[0]) * (right_eye[0] - left_eye[0]) + ((right_eye[1] - left_eye[1]) * (right_eye[1] - left_eye[1])))
                .toInt()
        val height = (Math.abs(
            (right_eye[1] - left_eye[1]) * middle_mouse[0] - (right_eye[0] - left_eye[0]) * middle_mouse[1] + right_eye[0] * left_eye[1] - right_eye[1] * left_eye[0]
        )
                / Math.pow(
            Math.pow(
                right_eye[1] - left_eye[1].toDouble(), 2.0
            ) + Math.pow(right_eye[0] - left_eye[0].toDouble(), 2.0), 0.5
        )).toInt()

        // Get Margin
        margin[0] = width * margin[0]
        margin[1] = height * margin[1]
        margin[2] = height * (1 + margin[2])

        // Get Base Line

        val atan_top =
            Math.atan((right_eye[1] - left_eye[1]) / (right_eye[0] - left_eye[0]).toDouble())
        val x_delta = Math.cos(atan_top)
        val y_delta = -Math.sin(atan_top)

        val base1 = doubleArrayOf(
            left_eye[0] - x_delta * margin[0],
            left_eye[1] + y_delta * margin[0]
        )

        val base2 = doubleArrayOf(
            right_eye[0] + x_delta * margin[0],
            right_eye[1] - y_delta * margin[0]
        )

        // Get Bounding Box
        val top_left = doubleArrayOf(
            base1[0] - y_delta * margin[1],
            base1[1] - x_delta * margin[1]
        )
        val top_right = doubleArrayOf(
            base2[0] - y_delta * margin[1],
            base2[1] - x_delta * margin[1]
        )
        val bottom_left = doubleArrayOf(
            base1[0] + y_delta * margin[2],
            base1[1] + x_delta * margin[2]
        )
        val bottom_right = doubleArrayOf(
            base2[0] + y_delta * margin[2],
            base2[1] + x_delta * margin[2]
        )

        // Get Max Width, Height
        val widthA = Math.sqrt(
            Math.pow(
                bottom_right[0] - bottom_left[0],
                2.0
            ) + Math.pow(bottom_right[1] - bottom_left[1], 2.0)
        )
        val widthB = Math.sqrt(
            Math.pow(
                top_right[0] - top_left[0],
                2.0
            ) + Math.pow(top_right[1] - top_left[1], 2.0)
        )
        val maxWidth = Math.max(widthA, widthB)
        val heightA = Math.sqrt(
            Math.pow(
                top_right[0] - bottom_right[0],
                2.0
            ) + Math.pow(top_right[1] - bottom_right[1], 2.0)
        )
        val heightB = Math.sqrt(
            Math.pow(
                top_left[0] - bottom_left[0],
                2.0
            ) + Math.pow(top_left[1] - bottom_left[1], 2.0)
        )
        val maxHeight = Math.max(heightA, heightB)

        // Prepare MAT for CV
        val srcPoints = arrayListOf(
            Point(top_left[0], top_left[1]),
            Point(top_right[0], top_right[1]),
            Point(bottom_right[0], bottom_right[1]),
            Point(bottom_left[0], bottom_left[1])
        )

        val dstPoints = arrayListOf(
            Point(0.0, 0.0),
            Point(maxWidth - 1, 0.0),
            Point(maxWidth - 1, maxHeight - 1),
            Point(0.0, maxHeight - 1)
        )

        // Transform and resize
        val matPt_src = MatOfPoint2f()
        val matPt_dst = MatOfPoint2f()
        matPt_src.fromList(srcPoints)
        matPt_dst.fromList(dstPoints)

        val srcimg = Mat(bm.width, bm.height, CvType.CV_8UC4)
        Utils.bitmapToMat(bm, srcimg)

        val tfm = Imgproc.getPerspectiveTransform(matPt_src, matPt_dst)
        val srcZeroMat = Mat.zeros(bm.width, bm.height, srcimg.type())

        Imgproc.warpPerspective(srcimg, srcZeroMat, tfm, Size(maxWidth, maxHeight))

        val result = Bitmap.createBitmap(
            srcZeroMat.width(),
            srcZeroMat.height(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(srcZeroMat, result)
        return resizeOp.apply(result)
    }

    private fun fourPointAlign(bm: Bitmap): Bitmap {
        if(landmarks.array().isEmpty())
            return bm.resize(targetSize.width.toInt(), targetSize.height.toInt())

        val left_eye = doubleArrayOf(landmarks.get(0), landmarks.get(1))
        val right_eye = doubleArrayOf(landmarks.get(2), landmarks.get(3))
        val mouse_left = doubleArrayOf(landmarks.get(6), landmarks.get(7))
        val mouse_right = doubleArrayOf(landmarks.get(8), landmarks.get(9))
        val middle_mouse = doubleArrayOf(
            (mouse_left[0] + mouse_right[0]) / 2,
            (mouse_left[1] + mouse_right[1]) / 2
        )

        // float margin[] = {0.5f, 0.57f, 0.57f};
        val margin = floatArrayOf(1.08f, 1.27f, 0.47f)

        // Get Width, Height
        val width =
            Math.sqrt((right_eye[0] - left_eye[0]) * (right_eye[0] - left_eye[0]) + ((right_eye[1] - left_eye[1]) * (right_eye[1] - left_eye[1])))
                .toInt()
        val height = (Math.abs(
            (right_eye[1] - left_eye[1]) * middle_mouse[0] - (right_eye[0] - left_eye[0]) * middle_mouse[1] + right_eye[0] * left_eye[1] - right_eye[1] * left_eye[0]
        )
                / Math.pow(
            Math.pow(
                right_eye[1] - left_eye[1].toDouble(), 2.0
            ) + Math.pow(right_eye[0] - left_eye[0].toDouble(), 2.0), 0.5
        )).toInt()

        // Get Margin
        margin[0] = width * margin[0]
        margin[1] = height * margin[1]
        margin[2] = height * (1 + margin[2])

        // Get Base Line
        val atan_top =
            Math.atan((right_eye[1] - left_eye[1]) / (right_eye[0] - left_eye[0]).toDouble())
        val x_delta = Math.cos(atan_top)
        val y_delta = -Math.sin(atan_top)

        val base1 = doubleArrayOf(
            left_eye[0] - x_delta * margin[0],
            left_eye[1] + y_delta * margin[0]
        )

        val base2 = doubleArrayOf(
            right_eye[0] + x_delta * margin[0],
            right_eye[1] - y_delta * margin[0]
        )

        // Get Bounding Box
        val top_left = doubleArrayOf(
            base1[0] - y_delta * margin[1],
            base1[1] - x_delta * margin[1]
        )
        val top_right = doubleArrayOf(
            base2[0] - y_delta * margin[1],
            base2[1] - x_delta * margin[1]
        )
        val bottom_left = doubleArrayOf(
            base1[0] + y_delta * margin[2],
            base1[1] + x_delta * margin[2]
        )
        val bottom_right = doubleArrayOf(
            base2[0] + y_delta * margin[2],
            base2[1] + x_delta * margin[2]
        )

        // Get Max Width, Height
        val widthA = Math.sqrt(
            Math.pow(
                bottom_right[0] - bottom_left[0],
                2.0
            ) + Math.pow(bottom_right[1] - bottom_left[1], 2.0)
        )
        val widthB = Math.sqrt(
            Math.pow(
                top_right[0] - top_left[0],
                2.0
            ) + Math.pow(top_right[1] - top_left[1], 2.0)
        )
        val maxWidth = Math.max(widthA, widthB)
        val heightA = Math.sqrt(
            Math.pow(
                top_right[0] - bottom_right[0],
                2.0
            ) + Math.pow(top_right[1] - bottom_right[1], 2.0)
        )
        val heightB = Math.sqrt(
            Math.pow(
                top_left[0] - bottom_left[0],
                2.0
            ) + Math.pow(top_left[1] - bottom_left[1], 2.0)
        )
        val maxHeight = Math.max(heightA, heightB)

        // Prepare MAT for CV
        val srcPoints = arrayListOf(
            Point(top_left[0], top_left[1]),
            Point(top_right[0], top_right[1]),
            Point(bottom_right[0], bottom_right[1]),
            Point(bottom_left[0], bottom_left[1])
        )

        val dstPoints = arrayListOf(
            Point(0.0, 0.0),
            Point(maxWidth - 1, 0.0),
            Point(maxWidth - 1, maxHeight - 1),
            Point(0.0, maxHeight - 1)
        )

        // Transform and resize
        val matPt_src = MatOfPoint2f()
        val matPt_dst = MatOfPoint2f()
        matPt_src.fromList(srcPoints)
        matPt_dst.fromList(dstPoints)

        val srcimg = Mat(bm.width, bm.height, CvType.CV_8UC4)
        Utils.bitmapToMat(bm, srcimg)

        val tfm = Imgproc.getPerspectiveTransform(matPt_src, matPt_dst)
        val srcZeroMat = Mat.zeros(bm.width, bm.height, srcimg.type())

        Imgproc.warpPerspective(srcimg, srcZeroMat, tfm, Size(maxWidth, maxHeight))

        val result = Bitmap.createBitmap(
            srcZeroMat.width(),
            srcZeroMat.height(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(srcZeroMat, result)
        return resizeOp.apply(result)

    }


    @Synchronized
    private fun fivePointAlign(bm: Bitmap): Bitmap {
        if(landmarks.array().isEmpty()){
            return bm.resize(targetSize.width.toInt(), targetSize.height.toInt())
        }

        val left_eye = doubleArrayOf(landmarks.get(0), landmarks.get(1))
        val right_eye = doubleArrayOf(landmarks.get(2), landmarks.get(3))
        val nose = doubleArrayOf(landmarks.get(4), landmarks.get(5))
        val mouse_left = doubleArrayOf(landmarks.get(6), landmarks.get(7))
        val mouse_right = doubleArrayOf(landmarks.get(8), landmarks.get(9))

        val middle_mouse = doubleArrayOf(
            (mouse_left[0] + mouse_right[0]) / 2,
            (mouse_left[1] + mouse_right[1]) / 2
        )

        /**
         * margin
         * side, top, nose, bottom
         */
        val margin = mutableMapOf(
            "side" to 1.08f,
            "top" to 1.27f,
            "nose" to 0.49f,
            "bottom" to 0.47f,
            "bottom_full" to 1.0f
        )

        // Get Width, Height
        val width =
            Math.sqrt((right_eye[0] - left_eye[0]) * (right_eye[0] - left_eye[0]) + ((right_eye[1] - left_eye[1]) * (right_eye[1] - left_eye[1])))
                .toInt()
        val height = (Math.abs(
            (right_eye[1] - left_eye[1]) * middle_mouse[0] - (right_eye[0] - left_eye[0]) * middle_mouse[1] + right_eye[0] * left_eye[1] - right_eye[1] * left_eye[0]
        )
                / Math.pow(
            Math.pow(
                right_eye[1] - left_eye[1], 2.0
            ) + Math.pow(right_eye[0] - left_eye[0], 2.0), 0.5
        )).toInt()

        // Get Margin
        margin["side"] = width * margin["side"]!!
        margin["top"] = height * margin["top"]!!
        margin["bottom"] = height * (1 + margin["bottom"]!!)
        margin["nose"] = height * margin["nose"]!!

        // Get Base Line
        val atan_top = Math.atan((right_eye[1] - left_eye[1]) / (right_eye[0] - left_eye[0]))
        val x_delta = Math.cos(atan_top)
        val y_delta = -Math.sin(atan_top)

        val base1 = doubleArrayOf(
            left_eye[0] - x_delta * margin["side"]!!,
            left_eye[1] + y_delta * margin["side"]!!
        )

        val base2 = doubleArrayOf(
            right_eye[0] + x_delta * margin["side"]!!,
            right_eye[1] - y_delta * margin["side"]!!
        )

        // Get Bounding Box
        val top_left = doubleArrayOf(
            base1[0] - y_delta * margin["top"]!!,
            base1[1] - x_delta * margin["top"]!!
        )
        val top_right = doubleArrayOf(
            base2[0] - y_delta * margin["top"]!!,
            base2[1] - x_delta * margin["top"]!!
        )
        val bottom_left = doubleArrayOf(
            base1[0] + y_delta * margin["bottom"]!!,
            base1[1] + x_delta * margin["bottom"]!!
        )
        val bottom_right = doubleArrayOf(
            base2[0] + y_delta * margin["bottom"]!!,
            base2[1] + x_delta * margin["bottom"]!!
        )

        // Get Max Width, Height
        val widthA = Math.sqrt(
            Math.pow(
                bottom_right[0] - bottom_left[0],
                2.0
            ) + Math.pow(bottom_right[1] - bottom_left[1], 2.0)
        )
        val widthB = Math.sqrt(
            Math.pow(
                top_right[0] - top_left[0],
                2.0
            ) + Math.pow(top_right[1] - top_left[1], 2.0)
        )
        val maxWidth = Math.max(widthA, widthB)
        val heightA = Math.sqrt(
            Math.pow(
                top_right[0] - bottom_right[0],
                2.0
            ) + Math.pow(top_right[1] - bottom_right[1], 2.0)
        )
        val heightB = Math.sqrt(
            Math.pow(
                top_left[0] - bottom_left[0],
                2.0
            ) + Math.pow(top_left[1] - bottom_left[1], 2.0)
        )
        val maxHeight = Math.max(heightA, heightB)

        // nose
        val nose_left = doubleArrayOf(
            base1[0] + y_delta * margin["nose"]!!,
            base1[1] + x_delta * margin["nose"]!!
        )
        val nose_right = doubleArrayOf(
            base2[0] + y_delta * margin["nose"]!!,
            base2[1] + x_delta * margin["nose"]!!
        )

        val upperHeight =
            ((margin["top"]!! + margin["nose"]!!) / (margin["top"]!! + margin["bottom"]!!) * maxHeight)
        val lowerHeight = maxHeight - upperHeight

        // Prepare MAT for CV
        val dst_upper = arrayListOf(
            Point(0.0, 0.0),
            Point(maxWidth - 1, 0.0),
            Point(maxWidth - 1, upperHeight - 1),
            Point(0.0, upperHeight - 1)
        )
        val src_upper = arrayListOf(
            Point(top_left[0], top_left[1]),
            Point(top_right[0], top_right[1]),
            Point(nose_right[0], nose_right[1]),
            Point(nose_left[0], nose_left[1])
        )

        // Transform and resize
        val matPt_src_upper = MatOfPoint2f()
        val matPt_dst_upper = MatOfPoint2f()
        matPt_src_upper.fromList(src_upper)
        matPt_dst_upper.fromList(dst_upper)

        val matUpper = Imgproc.getPerspectiveTransform(matPt_src_upper, matPt_dst_upper)
        val srcimg = Mat(bm.width, bm.height, CvType.CV_8UC4)
        Utils.bitmapToMat(bm, srcimg)

        val srcZeroMatUpper = Mat.zeros(bm.width, bm.height, srcimg.type())

        Imgproc.warpPerspective(
            srcimg,
            srcZeroMatUpper,
            matUpper,
            Size(maxWidth, upperHeight)
        )                           // warp_upper
        Imgproc.resize(
            srcZeroMatUpper, srcZeroMatUpper, Size(112.0, 71.0), 0.0, 0.0,
            Imgproc.INTER_AREA
        )   // resize_upper

        val dst_lower = arrayListOf(
            Point(0.0, 0.0),
            Point(maxWidth - 1, 0.0),
            Point(maxWidth - 1, lowerHeight - 1),
            Point(0.0, lowerHeight - 1)
        )
        val src_lower = arrayListOf(
            Point(nose_left[0], nose_left[1]),
            Point(nose_right[0], nose_right[1]),
            Point(bottom_right[0], bottom_right[1]),
            Point(bottom_left[0], bottom_left[1])
        )

        val matPt_src_lower = MatOfPoint2f()
        val matPt_dst_lower = MatOfPoint2f()
        matPt_src_lower.fromList(src_lower)
        matPt_dst_lower.fromList(dst_lower)

        val srcZeroMatLower = Mat.zeros(bm.width, bm.height, srcimg.type())
        val matLower = Imgproc.getPerspectiveTransform(matPt_src_lower, matPt_dst_lower)

        Imgproc.warpPerspective(
            srcimg,
            srcZeroMatLower,
            matLower,
            Size(maxWidth, lowerHeight)
        )                 // warp_upper
        Imgproc.resize(
            srcZeroMatLower, srcZeroMatLower, Size(112.0, 41.0), 0.0, 0.0,
            Imgproc.INTER_AREA
        )   // resize_upper

        val dstMat = Mat.zeros(targetSize.width.toInt(), targetSize.height.toInt(), srcimg.type())
        val result = Bitmap.createBitmap(
            targetSize.width.toInt(),
            targetSize.height.toInt(),
            Bitmap.Config.ARGB_8888
        )

        Core.vconcat(arrayListOf(srcZeroMatUpper, srcZeroMatLower), dstMat)
        Utils.matToBitmap(dstMat, result)
        return result
    }


    @Synchronized
    private fun fivePointAlignWithCrop(bm: Bitmap, landmarks: DoubleArray, targetSize: Size): Any? {
        try {
            val left_eye = doubleArrayOf(landmarks.get(0), landmarks.get(1))
            val right_eye = doubleArrayOf(landmarks.get(2), landmarks.get(3))
            val nose = doubleArrayOf(landmarks.get(4), landmarks.get(5))
            val mouse_left = doubleArrayOf(landmarks.get(6), landmarks.get(7))
            val mouse_right = doubleArrayOf(landmarks.get(8), landmarks.get(9))

            val middle_mouse = doubleArrayOf(
                (mouse_left[0] + mouse_right[0]) / 2,
                (mouse_left[1] + mouse_right[1]) / 2
            )


            /**
             * margin
             * side, top, nose, bottom
             */
            val margin = mutableMapOf(
                "side" to 1.08f,
                "top" to 1.27f,
                "nose" to 0.49f,
                "bottom" to 0.47f,
                "bottom_full" to 1.0f,
                "bottom_half" to 0.5f
            )

            // Get Width, Height
            val width =
                Math.sqrt((right_eye[0] - left_eye[0]) * (right_eye[0] - left_eye[0]) + ((right_eye[1] - left_eye[1]) * (right_eye[1] - left_eye[1])))
                    .toInt()
            val height = (Math.abs(
                (right_eye[1] - left_eye[1]) * middle_mouse[0] - (right_eye[0] - left_eye[0]) * middle_mouse[1] + right_eye[0] * left_eye[1] - right_eye[1] * left_eye[0]
            )
                    / Math.pow(
                Math.pow(
                    right_eye[1] - left_eye[1], 2.0
                ) + Math.pow(right_eye[0] - left_eye[0], 2.0), 0.5
            )).toInt()

            // Get Margin
            margin["side"] = width * margin["side"]!!
            margin["top"] = height * margin["top"]!!
            margin["bottom"] = height * (1 + margin["bottom"]!!)
            margin["nose"] = height * margin["nose"]!!
            margin["bottom_half"] = height * margin["bottom_half"]!!

            // Get Base Line
            val atan_top = Math.atan((right_eye[1] - left_eye[1]) / (right_eye[0] - left_eye[0]))
            val x_delta = Math.cos(atan_top)
            val y_delta = -Math.sin(atan_top)

            val base1 = doubleArrayOf(
                left_eye[0] - x_delta * margin["side"]!!,
                left_eye[1] + y_delta * margin["side"]!!
            )

            val base2 = doubleArrayOf(
                right_eye[0] + x_delta * margin["side"]!!,
                right_eye[1] - y_delta * margin["side"]!!
            )

            // Get Bounding Box
            val top_left = doubleArrayOf(
                base1[0] - y_delta * margin["top"]!!,
                base1[1] - x_delta * margin["top"]!!
            )
            val top_right = doubleArrayOf(
                base2[0] - y_delta * margin["top"]!!,
                base2[1] - x_delta * margin["top"]!!
            )
            val bottom_left = doubleArrayOf(
                base1[0] + y_delta * margin["bottom"]!!,
                base1[1] + x_delta * margin["bottom"]!!
            )
            val bottom_right = doubleArrayOf(
                base2[0] + y_delta * margin["bottom"]!!,
                base2[1] + x_delta * margin["bottom"]!!
            )

            // Get Max Width, Height
            val widthA = Math.sqrt(
                Math.pow(
                    bottom_right[0] - bottom_left[0],
                    2.0
                ) + Math.pow(bottom_right[1] - bottom_left[1], 2.0)
            )
            val widthB = Math.sqrt(
                Math.pow(
                    top_right[0] - top_left[0],
                    2.0
                ) + Math.pow(top_right[1] - top_left[1], 2.0)
            )
            val maxWidth = Math.max(widthA, widthB)
            val heightA = Math.sqrt(
                Math.pow(
                    top_right[0] - bottom_right[0],
                    2.0
                ) + Math.pow(top_right[1] - bottom_right[1], 2.0)
            )
            val heightB = Math.sqrt(
                Math.pow(
                    top_left[0] - bottom_left[0],
                    2.0
                ) + Math.pow(top_left[1] - bottom_left[1], 2.0)
            )
            val maxHeight = Math.max(heightA, heightB)

            // nose
            val nose_left = doubleArrayOf(
                base1[0] + y_delta * margin["nose"]!!,
                base1[1] + x_delta * margin["nose"]!!
            )
            val nose_right = doubleArrayOf(
                base2[0] + y_delta * margin["nose"]!!,
                base2[1] + x_delta * margin["nose"]!!
            )

            val upperHeight =
                ((margin["top"]!! + margin["nose"]!!) / (margin["top"]!! + margin["bottom"]!!) * maxHeight)
            val lowerHeight = maxHeight - upperHeight

            // Prepare MAT for CV
            val dst_upper = arrayListOf(
                Point(0.0, 0.0),
                Point(maxWidth - 1, 0.0),
                Point(maxWidth - 1, upperHeight - 1),
                Point(0.0, upperHeight - 1)
            )
            val src_upper = arrayListOf(
                Point(top_left[0], top_left[1]),
                Point(top_right[0], top_right[1]),
                Point(nose_right[0], nose_right[1]),
                Point(nose_left[0], nose_left[1])
            )

            // Transform and resize
            val matPt_src_upper = MatOfPoint2f()
            val matPt_dst_upper = MatOfPoint2f()
            matPt_src_upper.fromList(src_upper)
            matPt_dst_upper.fromList(dst_upper)

            val matUpper = Imgproc.getPerspectiveTransform(matPt_src_upper, matPt_dst_upper)
            val srcimg = Mat(bm.width, bm.height, CvType.CV_8UC4)
            Utils.bitmapToMat(bm, srcimg)

            val srcZeroMatUpper = Mat.zeros(bm.width, bm.height, srcimg.type())

            Imgproc.warpPerspective(
                srcimg,
                srcZeroMatUpper,
                matUpper,
                Size(maxWidth, upperHeight)
            )                           // warp_upper
            Imgproc.resize(
                srcZeroMatUpper, srcZeroMatUpper, Size(112.0, 71.0), 0.0, 0.0,
                Imgproc.INTER_AREA
            )   // resize_upper

            val dst_lower = arrayListOf(
                Point(0.0, 0.0),
                Point(maxWidth - 1, 0.0),
                Point(maxWidth - 1, lowerHeight - 1),
                Point(0.0, lowerHeight - 1)
            )
            val src_lower = arrayListOf(
                Point(nose_left[0], nose_left[1]),
                Point(nose_right[0], nose_right[1]),
                Point(bottom_right[0], bottom_right[1]),
                Point(bottom_left[0], bottom_left[1])
            )

            val matPt_src_lower = MatOfPoint2f()
            val matPt_dst_lower = MatOfPoint2f()
            matPt_src_lower.fromList(src_lower)
            matPt_dst_lower.fromList(dst_lower)

            val srcZeroMatLower = Mat.zeros(bm.width, bm.height, srcimg.type())
            val matLower = Imgproc.getPerspectiveTransform(matPt_src_lower, matPt_dst_lower)

            Imgproc.warpPerspective(
                srcimg,
                srcZeroMatLower,
                matLower,
                Size(maxWidth, lowerHeight)
            )                 // warp_upper
            Imgproc.resize(
                srcZeroMatLower, srcZeroMatLower, Size(112.0, 41.0), 0.0, 0.0,
                Imgproc.INTER_AREA
            )   // resize_upper

            val dstMat =
                Mat.zeros(targetSize.width.toInt(), targetSize.height.toInt(), srcimg.type())
            val alignReslt = Bitmap.createBitmap(
                targetSize.width.toInt(),
                targetSize.height.toInt(),
                Bitmap.Config.ARGB_8888
            )

            Core.vconcat(arrayListOf(srcZeroMatUpper, srcZeroMatLower), dstMat)
            Utils.matToBitmap(dstMat, alignReslt)

            margin["bottom_full"] = height * (1 + margin["bottom_full"]!!)
            val bottom_full_left = doubleArrayOf(
                base1[0]!! + y_delta * margin["bottom_full"]!!,
                base1[1] + x_delta * margin["bottom_full"]!!
            )
            val bottom_full_right = doubleArrayOf(
                base2[0]!! + y_delta * margin["bottom_full"]!!,
                base2[1] + x_delta * margin["bottom_full"]!!
            )
            val height_fullA = Math.sqrt(
                Math.pow(
                    top_right[0] - bottom_full_right[0],
                    2.0
                ) + Math.pow(top_right[1] - bottom_full_right[1], 2.0)
            )
            val height_fullB = Math.sqrt(
                Math.pow(
                    top_left[0] - bottom_full_left[0],
                    2.0
                ) + Math.pow(top_left[1] - bottom_full_left[1], 2.0)
            )
            val maxHeight_full = Math.max(height_fullA, height_fullB)

            // Prepare MAT for CV
            val dst_full = arrayListOf(
                Point(0.0, 0.0),
                Point(maxWidth - 1, 0.0),
                Point(maxWidth - 1, maxHeight_full - 1),
                Point(0.0, maxHeight_full - 1)
            )
            val src_full = arrayListOf(
                Point(top_left),
                Point(top_right),
                Point(bottom_full_right),
                Point(bottom_full_left)
            )

            val matPt_srcFull = MatOfPoint2f()
            val matPt_dstFull = MatOfPoint2f()
            matPt_srcFull.fromList(src_full)
            matPt_dstFull.fromList(dst_full)

            // Transform and resize
            val M_full = Imgproc.getPerspectiveTransform(matPt_srcFull, matPt_dstFull)
            val srcZeroMat = Mat.zeros(bm.width, bm.height, srcimg.type())

            Imgproc.warpPerspective(srcimg, srcZeroMat, M_full, Size(maxWidth, maxHeight_full))
            Imgproc.resize(
                srcZeroMat, srcZeroMat, Size(500.0, ((500 * maxHeight_full) / maxWidth)), 0.0, 0.0,
                Imgproc.INTER_AREA
            )   // resize_upper

            val cropReslt = Bitmap.createBitmap(
                500,
                ((500 * maxHeight_full) / maxWidth).toInt(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(srcZeroMat, cropReslt)

            // blur
            val bottom_half_left = doubleArrayOf(
                base1[0]!! + y_delta * margin["bottom_half"]!!,
                base1[1] + x_delta * margin["bottom_half"]!!
            )
            val bottom_half_right = doubleArrayOf(
                base2[0]!! + y_delta * margin["bottom_half"]!!,
                base2[1] + x_delta * margin["bottom_half"]!!
            )
            val height_halfA = Math.sqrt(
                Math.pow(
                    top_right[0] - bottom_half_right[0],
                    2.0
                ) + Math.pow(top_right[1] - bottom_half_right[1], 2.0)
            )
            val height_halfB = Math.sqrt(
                Math.pow(
                    top_left[0] - bottom_half_left[0],
                    2.0
                ) + Math.pow(top_left[1] - bottom_half_left[1], 2.0)
            )
            val maxHeight_half = Math.max(height_halfA, height_halfB)

            val dst_half = arrayListOf(
                Point(0.0, 0.0),
                Point(maxWidth - 1, 0.0),
                Point(maxWidth - 1, maxHeight_half - 1),
                Point(0.0, maxHeight_half - 1)
            )
            val src_half = arrayListOf(
                Point(top_left),
                Point(top_right),
                Point(bottom_half_right),
                Point(bottom_half_left)
            )

            val matPt_srcHalf = MatOfPoint2f()
            val matPt_dstHalf = MatOfPoint2f()
            matPt_srcHalf.fromList(src_half)
            matPt_dstHalf.fromList(dst_half)


            // Transform and resize
            val M_half = Imgproc.getPerspectiveTransform(matPt_srcHalf, matPt_dstHalf)
            val srcZeroMatHalf = Mat.zeros(bm.width, bm.height, srcimg.type())
            Imgproc.warpPerspective(srcimg, srcZeroMatHalf, M_half, Size(maxWidth, maxHeight_half))
            Imgproc.resize(
                srcZeroMatHalf,
                srcZeroMatHalf,
                Size(500.0, ((500 * maxHeight_half) / maxWidth)),
                0.0,
                0.0,
                Imgproc.INTER_AREA
            )   // resize_upper

            val cropHalfReslt = Bitmap.createBitmap(
                500,
                ((500 * maxHeight_half) / maxWidth).toInt(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(srcZeroMatHalf, cropHalfReslt)

            return arrayOf(alignReslt, cropReslt, cropHalfReslt)

        } catch (e: Exception) {
            return null
        }
    }

}

