package com.nota.nota_sdk.ai.tensorflow.face.mtcnn

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import com.nota.nota_sdk.ai.common.ModelInfo
import com.nota.nota_sdk.ai.tensorflow.common.TensorflowModel
import com.nota.nota_sdk.vision.face.Face
import com.nota.nota_sdk.vision.face.internal.FaceDetectorImpl
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt
import android.content.Context
import com.jwhae.billboard.ai.tensorflow.face.mtcnn.Box
import com.jwhae.billboard.ai.tensorflow.face.mtcnn.Utils

class MTCNN(context:Context) : TensorflowModel<Array<Face>>(context, ModelInfo.Builder(modelName = "mtcnn.pb", isEncrypted = false).build()), FaceDetectorImpl {
    companion object {
        private const val TAG = "MTCNN"

        // const
        private const val pNetThreshold = 0.6f
        private const val rNetThreshold = 0.7f
        private const val oNetThreshold = 0.7f

        private const val minFaceSizeFactor = 0.5f
        private const val factor = 0.709f

        //tensor name
        private const val pNetInName = "pnet/input:0"
        private const val rNetInName = "rnet/input:0"
        private const val oNetInName = "onet/input:0"
        private val pNetOutName = arrayOf("pnet/prob1:0", "pnet/conv4-2/BiasAdd:0")
        private val rNetOutName = arrayOf("rnet/prob1:0", "rnet/conv5-2/conv5-2:0")
        private val oNetOutName =
            arrayOf("onet/prob1:0", "onet/conv6-2/conv6-2:0", "onet/conv6-3/conv6-3:0")
    }

    override fun detect(var1: Bitmap): Array<Face> {
        return run(var1)
    }

    override fun initParameters() {}

    override fun processOutput(): Array<Face> { return ArrayList<Face>().toTypedArray() }

    override fun run(bm: Bitmap): Array<Face> {
        //【1】PNet generate candidate boxes
        var boxes = pNet(bm, (bm.width * minFaceSizeFactor).toInt())
        squareLimit(boxes, bm.width, bm.height)

        //【2】RNet
        boxes = rNet(bm, boxes)
        squareLimit(boxes, bm.width, bm.height)

        //【3】ONet
        boxes = oNet(bm, boxes)
        boxes.sortByDescending { it.area() }

        return transformOutput(boxes.clone() as Array<Box>)
    }

    private fun transformOutput(list : Array<Box>) : Array<Face>{
        val _list = ArrayList<Face>()

        for(bbox in list){
            _list.add(Face(rectf = bbox.transform2RectF(), landmarks = bbox.return2PointF()))
        }

        return  _list.toTypedArray()
    }

    // --------- MTCNN Implementation ----------

    //Flip before input, and flip output
    private fun pNetForward(
        bitmap: Bitmap,
        PNetOutProb: Array<FloatArray>,
        PNetOutBias: Array<Array<FloatArray>>
    ): Int {
        val w = bitmap.width
        val h = bitmap.height
        val pNetIn = bitmapToFloatArray(bitmap)
        Utils.flipDiag(
            pNetIn,
            h,
            w,
            3
        ) //Flip diagonally
        interpreter.feed(
            pNetInName,
            pNetIn,
            1,
            w.toLong(),
            h.toLong(),
            3
        )
        interpreter.run(
            pNetOutName,
            false
        )
        val pNetOutSizeW = ceil(w * 0.5 - 5).toInt()
        val pNetOutSizeH = ceil(h * 0.5 - 5).toInt()
        val pNetOutP = FloatArray(pNetOutSizeW * pNetOutSizeH * 2)
        val pNetOutB = FloatArray(pNetOutSizeW * pNetOutSizeH * 4)
        interpreter.fetch(
            pNetOutName[0], pNetOutP
        )
        interpreter.fetch(
            pNetOutName[1], pNetOutB
        )
        //[Writing one] First flip, then convert to 2/3-dimensional array
        Utils.flipDiag(
            pNetOutP,
            pNetOutSizeW,
            pNetOutSizeH,
            2
        )
        Utils.flipDiag(
            pNetOutB,
            pNetOutSizeW,
            pNetOutSizeH,
            4
        )
        Utils.expand(pNetOutB, PNetOutBias)
        Utils.expandProb(
            pNetOutP,
            PNetOutProb
        )

        return 0
    }

    //Non-Maximum Suppression
    private fun nms(
        boxes: Vector<Box>,
        threshold: Float,
        method: String
    ) {
        for (i in boxes.indices) {
            val box = boxes[i]
            if (!box.deleted) {
                //score < 0 Indicates that the current rectangular frame is deleted
                for (j in i + 1 until boxes.size) {
                    val box2 = boxes[j]
                    if (!box2.deleted) {
                        val x1 = box.box[0].coerceAtLeast(box2.box[0])
                        val y1 = box.box[1].coerceAtLeast(box2.box[1])
                        val x2 = box.box[2].coerceAtMost(box2.box[2])
                        val y2 = box.box[3].coerceAtMost(box2.box[3])
                        if (x2 < x1 || y2 < y1) continue
                        val areaIoU = (x2 - x1 + 1) * (y2 - y1 + 1)
                        var iou = 0f
                        if (method == "Union") iou =
                            1.0f * areaIoU / (box.area() + box2.area() - areaIoU) else if (method == "Min") {
                            iou = 1.0f * areaIoU / box.area().coerceAtMost(box2.area())

                        }
                        if (iou >= threshold) { // Delete the box with the smaller prob
                            if (box.score > box2.score) box2.deleted = true else box.deleted = true
                        }
                    }
                }
            }
        }
    }

    private fun generateBoxes(
        prob: Array<FloatArray>,
        bias: Array<Array<FloatArray>>,
        scale: Float,
        boxes: Vector<Box>
    ): Int {
        val h = prob.size
        val w: Int = prob[0].size

        for (y in 0 until h) for (x in 0 until w) {
            val score = prob[y][x]
            //only accept prob > threshold(0.6 here)
            if (score > pNetThreshold) {
                val box =
                    Box()
                //score
                box.score = score
                //box
                box.box[0] = (x * 2 / scale).roundToInt()
                box.box[1] = (y * 2 / scale).roundToInt()
                box.box[2] = ((x * 2 + 11) / scale).roundToInt()
                box.box[3] = ((y * 2 + 11) / scale).roundToInt()
                //bbr
                for (i in 0..3) box.bbr[i] = bias[y][x][i]
                //add
                boxes.addElement(box)
            }
        }
        return 0
    }

    private fun boundingBoxReggression(boxes: Vector<Box>) {
        for (i in boxes.indices) boxes[i].calibrate()
    }

    //Pnet + Bounding Box Regression + Non-Maximum Regression
    /* Regression is executed after NMS is executed
     * (1) For each scale , use NMS with threshold=0.5
     * (2) For all candidates , use NMS with threshold=0.7
     * (3) Calibrate Bounding Box
     * Note: The top line of the CNN input picture, the coordinate is [0..width,0].
     * Therefore, Bitmap needs to be folded in half before running the network;
     * the network output is the same.
     */
    private fun pNet(bitmap: Bitmap, minSize: Int): Vector<Box> {
        val whMin = Math.min(bitmap.width, bitmap.height)
        var currentFaceSize = minSize.toFloat() //currentFaceSize=minSize/(factor^k) k=0,1,2... until exceed whMin
        val totalBoxes = Vector<Box>()

        //【1】Image Paramid and Feed to Pnet
        while (currentFaceSize <= whMin) {
            val scale = 12.0f / currentFaceSize
            //(1)Image Resize
            val bm: Bitmap = bitmapResize(bitmap, scale)
            val w = bm.width
            val h = bm.height

            //(2)RUN CNN
            val pNetOutSizeW = (ceil(w * 0.5 - 5) + 0.5).toInt()
            val pNetOutSizeH = (ceil(h * 0.5 - 5) + 0.5).toInt()
            val pNetOutProb = Array(pNetOutSizeH) { FloatArray(pNetOutSizeW) }
            val pNetOutBias = Array(pNetOutSizeH) { Array(pNetOutSizeW) { FloatArray(4) } }
            pNetForward(bm, pNetOutProb, pNetOutBias)

            //(3)Data analysis
            val curBoxes =
                Vector<Box>()
            generateBoxes(pNetOutProb, pNetOutBias, scale, curBoxes)

            //(4)nms 0.5
            nms(curBoxes, 0.5f, "Union")

            //(5)add to totalBoxes
            for (i in curBoxes.indices) if (!curBoxes[i].deleted) totalBoxes.addElement(
                curBoxes[i]
            )
            //Face Size Proportionally increasing
            currentFaceSize /= factor
        }
        //NMS 0.7
        nms(totalBoxes, 0.7f, "Union")

        //BBR
        boundingBoxReggression(totalBoxes)
        return Utils.updateBoxes(totalBoxes)
    }

    // Intercept the rectangular frame specified in the box (cross-border processing),
    // and resize to size*size, and store the returned data in data.
    private fun cropAndResize(bitmap: Bitmap, box: Box, size: Int, data: FloatArray) {
        //(2)crop and resize
        val matrix = Matrix()
        val scale = 1.0f * size / box.width()
        matrix.postScale(scale, scale)
        val croped = Bitmap.createBitmap(
            bitmap,
            box.left(),
            box.top(),
            box.width(),
            box.height(),
            matrix,
            true
        )
        //(3)save
        val pixelsBuf = IntArray(size * size)
        croped.getPixels(
            pixelsBuf,
            0,
            croped.width,
            0,
            0,
            croped.width,
            croped.height
        )
        val imageMean = 127.5f
        val imageStd = 128f
        for (i in pixelsBuf.indices) {
            val `val` = pixelsBuf[i]
            data[i * 3 + 0] = ((`val` shr 16 and 0xFF) - imageMean) / imageStd
            data[i * 3 + 1] = ((`val` shr 8 and 0xFF) - imageMean) / imageStd
            data[i * 3 + 2] = ((`val` and 0xFF) - imageMean) / imageStd
        }
    }

    /*
     * RNET runs a neural network and writes score and bias into boxes
     */
    private fun rNetForward(
        RNetIn: FloatArray,
        boxes: Vector<Box>
    ) {
        val num = RNetIn.size / 24 / 24 / 3
        //feed & run
        interpreter.feed(
            rNetInName,
            RNetIn,
            num.toLong(),
            24,
            24,
            3
        )
        interpreter.run(
            rNetOutName,
            false
        )
        //fetch
        val rNetP = FloatArray(num * 2)
        val rNetB = FloatArray(num * 4)
        interpreter.fetch(
            rNetOutName[0], rNetP
        )
        interpreter.fetch(
            rNetOutName[1], rNetB
        )
        //转换
        for (i in 0 until num) {
            boxes[i].score = rNetP[i * 2 + 1]
            for (j in 0..3) boxes[i].bbr[j] = rNetB[i * 4 + j]
        }
    }

    //Refine Net
    private fun rNet(bitmap: Bitmap, boxes: Vector<Box>): Vector<Box> {
        //RNet Input Init
        val num = boxes.size
        val rNetIn = FloatArray(num * 24 * 24 * 3)
        val curCrop = FloatArray(24 * 24 * 3)
        var rNetInIdx = 0
        for (i in 0 until num) {
            cropAndResize(bitmap, boxes[i], 24, curCrop)
            Utils.flipDiag(curCrop, 24, 24, 3)

            for (j in curCrop.indices) rNetIn[rNetInIdx++] = curCrop[j]
        }
        //Run RNet
        rNetForward(rNetIn, boxes)
        //RNetThreshold
        for (i in 0 until num) if (boxes[i].score < rNetThreshold) boxes[i].deleted = true
        //Nms
        nms(boxes, 0.7f, "Union")
        boundingBoxReggression(boxes)
        return Utils.updateBoxes(boxes)
    }

    /*
     * ONet runs a neural network and writes score and bias into boxes
     */
    private fun oNetForward(
        ONetIn: FloatArray,
        boxes: Vector<Box>
    ) {
        val num = ONetIn.size / 48 / 48 / 3
        //feed & run
        interpreter.feed(
            oNetInName,
            ONetIn,
            num.toLong(),
            48,
            48,
            3
        )
        interpreter.run(
            oNetOutName,
            false
        )
        //fetch
        val oNetP = FloatArray(num * 2) //prob
        val oNetB = FloatArray(num * 4) //bias
        val oNetL = FloatArray(num * 10) //landmark
        interpreter.fetch(
            oNetOutName[0], oNetP
        )
        interpreter.fetch(
            oNetOutName[1], oNetB
        )
        interpreter.fetch(
            oNetOutName[2], oNetL
        )

        for (i in 0 until num) {
            //prob
            boxes[i].score = oNetP[i * 2 + 1]
            //bias
            for (j in 0..3) boxes[i].bbr[j] = oNetB[i * 4 + j]

            //landmark
            for (j in 0..4) {
                val x =
                    boxes[i].left() + (oNetL[i * 10 + j] * boxes[i].width()).toInt()
                val y = boxes[i].top() + (oNetL[i * 10 + j + 5] * boxes[i]
                    .height()).toInt()
                boxes[i].landmark[j] = Point(x, y)
            }
        }
    }

    //ONet
    private fun oNet(
        bitmap: Bitmap,
        boxes: Vector<Box>
    ): Vector<Box> {
        //ONet Input Init
        val num = boxes.size
        val oNetIn = FloatArray(num * 48 * 48 * 3)
        val curCrop = FloatArray(48 * 48 * 3)
        var oNetInIdx = 0
        for (i in 0 until num) {
            cropAndResize(bitmap, boxes[i], 48, curCrop)
            Utils.flipDiag(
                curCrop,
                48,
                48,
                3
            )
            for (j in curCrop.indices) oNetIn[oNetInIdx++] = curCrop[j]
        }
        //Run ONet
        oNetForward(oNetIn, boxes)
        //ONetThreshold
        for (i in 0 until num) if (boxes[i].score < oNetThreshold) boxes[i].deleted = true
        boundingBoxReggression(boxes)
        //Nms
        nms(boxes, 0.7f, "Min")
        return Utils.updateBoxes(boxes)
    }

    private fun squareLimit(
        boxes: Vector<Box>,
        w: Int,
        h: Int
    ) {
        //square
        for (i in boxes.indices) {
            boxes[i].toSquareShape()
            boxes[i].limitSquare(w, h)
        }
    }

    /*
       Detect faces, minSize is the smallest face pixel value
     */
    private fun bitmapResize(
        bm: Bitmap,
        scale: Float
    ): Bitmap {
        val width = bm.width
        val height = bm.height

        // CREATE A MATRIX FOR THE MANIPULATION。
        // matrix specify image affine transformation parameters
        val matrix = Matrix()

        // RESIZE THE BITMAP
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, true
        )
    }

    private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray? {
        val w = bitmap.width
        val h = bitmap.height
        val floatValues = FloatArray(w * h * 3)
        val intValues = IntArray(w * h)
        bitmap.getPixels(intValues, 0, bitmap.width,
            0, 0,
            bitmap.width,
            bitmap.height
        )

        val imageMean = 127.5f
        val imageStd = 128f

        for (i in intValues.indices) {
            val `val` = intValues[i]
            floatValues[i * 3 + 0] = ((`val` shr 16 and 0xFF) - imageMean) / imageStd
            floatValues[i * 3 + 1] = ((`val` shr 8 and 0xFF) - imageMean) / imageStd
            floatValues[i * 3 + 2] = ((`val` and 0xFF) - imageMean) / imageStd
        }
        return floatValues
    }

}