package com.jwhae.billboard.ai.tflite.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.jwhae.billboard.vision.face.detection.FaceDetector
import com.nota.nota_sdk.ai.common.ModelInfo
import com.nota.nota_sdk.ai.common.Platform
import com.nota.nota_sdk.ai.common.process.NonMaxSuppression
import com.nota.nota_sdk.ai.common.process.internal.Anchor
import com.nota.nota_sdk.ai.common.process.internal.IndexedScore
import com.nota.nota_sdk.ai.common.process.internal.NMS
import com.nota.nota_sdk.ai.support.image.ImageProcessor
import com.nota.nota_sdk.ai.support.image.common.internal.DataType
import com.nota.nota_sdk.ai.tflite.common.TfliteExcutionOption
import com.nota.nota_sdk.ai.tflite.common.TfliteExecutor
import com.nota.nota_sdk.ai.tflite.common.TfliteModel
import com.nota.nota_sdk.vision.face.Detection
import com.nota.nota_sdk.vision.face.Face
import com.nota.nota_sdk.vision.face.internal.FaceDetectorImpl
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.FloatBuffer

class BlazeFace : TfliteModel<Array<Face>>, FaceDetectorImpl {

    private lateinit var stride : IntArray

    private var minScale : Float = -1f
    private var maxScale : Float = -1f

    private var xScale : Float = -1f
    private var yScale : Float = -1f
    private var hScale : Float = -1f
    private var wScale : Float = -1f

    private var numBoxes = 0
    private var numCoordinates = 0

    private var nonMaxSuppression = NonMaxSuppression(NMS(inputWidth, inputHeight, MIN_SUPPRESSION_THRESHOLD))

    override val imageProcessor: ImageProcessor<TensorImage>
        get() = ImageProcessor.Builder<TensorImage>(Platform.Tflite, DataType.FLOAT32)
            .add(ResizeOp(interpreter.getInputTensor(0).shape().get(1), interpreter.getInputTensor(0).shape().get(2), ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(1f, 127.5f))
            .build()

    constructor(context: Context, exeOption : TfliteExcutionOption? = null) : super(context, ModelInfo.Builder(modelName = FRONT_MODEL, isEncrypted = false).build(), exeOption)

    private lateinit var anchors : ArrayList<Anchor>

    override fun initParameters() {
        anchors = ArrayList()

        if(modelInfo.modelName == FRONT_MODEL){
            stride = intArrayOf(8, 16, 16, 16)
            minScale = FRONT_MIN_SCALE
            maxScale = FRONT_MAX_SCALE
            xScale = FRONT_X_SCALE
            yScale = FRONT_Y_SCALE
            hScale = FRONT_H_SCALE
            wScale = FRONT_W_SCALE
        }else{
            stride = intArrayOf(16, 32, 32, 32)
            minScale = BACK_MIN_SCALE
            maxScale = BACK_MAX_SCALE
            xScale = BACK_X_SCALE
            yScale = BACK_Y_SCALE
            hScale = BACK_H_SCALE
            wScale = BACK_W_SCALE
        }

        numBoxes = interpreter.getOutputTensor(INDEX_BOX).shape()[1]
        numCoordinates = interpreter.getOutputTensor(INDEX_BOX).shape()[2]

        setAnchors()
    }

    private fun setAnchors(){
        var layerId = 0

        while (layerId < stride.size) {
            val anchorHeight = ArrayList<Float>()
            val anchorWidth = ArrayList<Float>()
            val aspectRatios = ArrayList<Float>()
            val scales = ArrayList<Float>()

            // For same strides, we merge the anchors in the same order.
            var lastSameStrideLayer = layerId

            while (lastSameStrideLayer < stride.size && stride.get(lastSameStrideLayer) == stride.get(layerId)) {
                val scale: Float = calculateScale(minScale, maxScale, lastSameStrideLayer, stride.size)

                for (aspectRatioId in 0 until ASPECT_RATIOS_SIZE) {
                    aspectRatios.add(1.0f)
                    scales.add(scale)
                }

                val scaleNext = if (lastSameStrideLayer == stride.size - 1) 1.0f else calculateScale(minScale, maxScale, lastSameStrideLayer + 1, stride.size)

                scales.add(Math.sqrt(scale * scaleNext.toDouble()).toFloat())
                aspectRatios.add(1.0f)

                lastSameStrideLayer += 1
            }

            for (i in aspectRatios.indices) {
                val ratio_sqrts = Math.sqrt(aspectRatios[i].toDouble()).toFloat()
                anchorHeight.add(scales[i] / ratio_sqrts)
                anchorWidth.add(scales[i] * ratio_sqrts)
            }

            val stride: Int = stride.get(layerId)
            val featureMapHeight = Math.ceil(1.0f * inputHeight / stride.toDouble()).toInt()
            val featureMapWidth = Math.ceil(1.0f * inputWidth / stride.toDouble()).toInt()

            for (y in 0 until featureMapHeight) {
                for (x in 0 until featureMapWidth) {
                    for (anchor_id in anchorHeight.indices) {
                        // TODO: Support specifying anchor_offset_x, anchor_offset_y.
                        val xCenter: Float = (x + ANCHOR_OFFSET_X) * 1.0f / featureMapWidth
                        val yCenter: Float = (y + ANCHOR_OFFSET_Y) * 1.0f / featureMapHeight

                        val newAnchor =
                            Anchor(
                                xCenter = xCenter,
                                yCenter = yCenter,
                                width = 1f,
                                height = 1f
                            )
                        anchors.add(newAnchor)
                    }
                }
            }
            layerId = lastSameStrideLayer
        }
    }

    private fun calculateScale(minScale: Float, maxScale: Float, strideIndex: Int, numStrides: Int): Float {
        return minScale + (maxScale - minScale) * 1.0f * strideIndex / (numStrides - 1.0f)
    }

    override val executor: TfliteExecutor
        get() = TfliteExecutor.Builder(interpreter, inferenceOutput)
            .setInputProcessor(imageProcessor)
            .build()

    override fun setModelOutput(interpreter: Interpreter) {
        inferenceOutput.clear()

        this.inferenceOutput[INDEX_BOX] = interpreter.getOutputTensor(INDEX_BOX).numBytes().toFloatBuffer().rewind() as FloatBuffer
        this.inferenceOutput[INDEX_SCORE] = interpreter.getOutputTensor(INDEX_SCORE).numBytes().toFloatBuffer().rewind() as FloatBuffer
    }

    override fun detect(var1: Bitmap) : Array<Face> {
        return run(var1)
    }

    override fun processOutput(): Array<Face> {
        if(!inferenceOutput.containsKey(INDEX_BOX) && !inferenceOutput.containsKey(INDEX_SCORE))
            throw NullPointerException("output not set")

        return locateFaceFromBitmap().toTypedArray()
    }

    private fun locateFaceFromBitmap() : ArrayList<Face>{
        val detections: MutableList<Detection> = ArrayList()

        val outBoxes = (inferenceOutput[INDEX_BOX] as FloatBuffer).flip() as FloatBuffer
        val outScores = (inferenceOutput[INDEX_SCORE] as FloatBuffer).flip() as FloatBuffer

        for (i in 0 until numBoxes) {

            if(outScores.limit() == 0) return java.util.ArrayList()

            var score: Float = outScores.get(i)
            score = if (score < -100.0f) -100.0f else score
            score = if (score > 100.0f) 100.0f else score
            score = 1.0f / (1.0f + Math.exp(-score.toDouble()).toFloat())

            if (score <= MIN_SCORE_THRESH) continue

            val face = decodeBoundingBox(i, outBoxes)
            face?.let { detections.add(Detection(face, score)) }
        }

        outScores.clear()
        outBoxes.clear()

        // Check if there are any detections at all.
        if (detections.isEmpty())
            return ArrayList()

        // Copy all the scores (there is a single score in each detection after
        // the above pruning) to an indexed vector for sorting. The first value is
        // the index of the detection in the original vector from which the score
        // stems, while the second is the actual score.
        val indexedScores: ArrayList<IndexedScore> = ArrayList()
        for (index in detections.indices) {
            indexedScores.add(
                IndexedScore(index, detections[index].score)
            )
        }

        indexedScores.sortedByDescending { it.score }

        // A set of detections and locations, wrapping the location data from each
        // detection, which are retained after the non-maximum suppression.
        return nonMaxSuppression.process(indexedScores, detections)
    }

    private fun decodeBoundingBox(indx : Int, outputBoxes : FloatBuffer) : Face? {
        val offset = numCoordinates * indx

        var xCenter = outputBoxes.get(offset + 0)
        var yCenter = outputBoxes.get(offset + 1)
        var w = outputBoxes.get(offset + 2)
        var h = outputBoxes.get(offset + 3)

        if( anchors.size < indx) return null

        xCenter = xCenter / xScale * anchors.get(indx).width + anchors.get(indx).xCenter
        yCenter = yCenter / yScale * anchors.get(indx).height + anchors.get(indx).yCenter
        h = h / hScale * anchors.get(indx).height
        w = w / wScale * anchors.get(indx).width

        val yMin = (yCenter - h / 2f)
        val xMin = (xCenter - w / 2f)
        val yMax = (yCenter + h / 2f)
        val xMax = (xCenter + w / 2f)

        val keyPoints = ArrayList<PointF>()

        for(i in 0 until 6){
            var keyPointOffset = indx * numCoordinates + 4 + i * 2
            val keyPointX = outputBoxes.get(keyPointOffset) / xScale * anchors[i].width + anchors[i].xCenter * inputWidth
            val keyPointY = outputBoxes.get(keyPointOffset + 1) / yScale * anchors[i].height + anchors[i].yCenter * inputHeight

            keyPoints.add(PointF(keyPointX, keyPointY))
        }

        return Face(
            RectF(xMin, yMin, xMax, yMax),
            keyPoints
        )
    }


    companion object{
        const val FRONT_MODEL = "face_detection_front.tflite"
        const val BACK_MODEL  = "face_detection_back.tflite"

        const val MIN_SUPPRESSION_THRESHOLD = 0.3f

        private val INDEX_BOX = 0
        private val INDEX_SCORE = 1

        private val ASPECT_RATIOS_SIZE = 1

        private val MIN_SCORE_THRESH = 0.5f

        private val ANCHOR_OFFSET_X = 0.5f
        private val ANCHOR_OFFSET_Y = 0.5f

        private val FRONT_MIN_SCALE = 0.1484375f
        private val FRONT_MAX_SCALE = 0.75f

        private val FRONT_X_SCALE = 128f
        private val FRONT_Y_SCALE = 128f
        private val FRONT_H_SCALE = 128f
        private val FRONT_W_SCALE = 128f

        private val BACK_MIN_SCALE = 0.15625f
        private val BACK_MAX_SCALE = 0.75f

        private val BACK_X_SCALE = 256f
        private val BACK_Y_SCALE = 256f
        private val BACK_H_SCALE = 256f
        private val BACK_W_SCALE = 256f
    }
}
