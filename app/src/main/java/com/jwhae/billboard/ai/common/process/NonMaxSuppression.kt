package com.nota.nota_sdk.ai.common.process

import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.nota.nota_sdk.ai.common.process.internal.IndexedScore
import com.nota.nota_sdk.ai.common.process.internal.NMS
import com.nota.nota_sdk.vision.face.Detection
import com.nota.nota_sdk.vision.face.Face
import kotlin.jvm.Throws

class NonMaxSuppression( val nmsParam : NMS) {
    @Throws(TypeCastException::class)
    @Synchronized
    internal fun process(vararg opInput: Any): ArrayList<Face> {
        val indexedScore = (opInput[0] as ArrayList<IndexedScore>).clone() as ArrayList<IndexedScore>
        val detections = (opInput[1] as ArrayList<Detection>).clone() as ArrayList<Detection>

        val remained: MutableList<IndexedScore> = ArrayList()
        val candidates: MutableList<IndexedScore> = ArrayList()
        val outputFaces: ArrayList<Face> = ArrayList()

        while (indexedScore.isNotEmpty()) {
            val (face, score) = detections[indexedScore[0].index]

            if (score <- 1f){
                break
            }

            remained.clear()
            candidates.clear()

            val location = RectF(face.rectf)

            // This includes the first box.
            for (indxScore in indexedScore) {
                val rest_location = RectF(detections[indxScore.index].face.rectf)

                val similarity: Float = overlapSimilarity(rest_location, location)

                if (similarity > nmsParam.minThreshold) {
                    candidates.add(indxScore)
                } else {
                    remained.add(indxScore)
                }
            }

            val weightedFace = face

            if (!candidates.isEmpty()) {
                var w_xmin = 0.0f
                var w_ymin = 0.0f
                var w_xmax = 0.0f
                var w_ymax = 0.0f
                var total_score = 0.0f

                var pointArray = arrayOf(PointF(), PointF(), PointF(), PointF(), PointF())

                for (candidate in candidates) {
                    total_score += candidate.score
                    val bbox = detections[candidate.index].face.rectf
                    w_xmin += bbox.left * candidate.score
                    w_ymin += bbox.top * candidate.score
                    w_xmax += bbox.right * candidate.score
                    w_ymax += bbox.bottom * candidate.score

                    val landmark = detections[candidate.index].face.landmarks
                    for(i in pointArray.indices){
                        pointArray[i].x += landmark[i].x
                        pointArray[i].y += landmark[i].y
                    }
                }

                weightedFace.rectf.left = (w_xmin / total_score * nmsParam.imageWidth)
                weightedFace.rectf.top = (w_ymin / total_score * nmsParam.imageHeight)
                weightedFace.rectf.right = (w_xmax / total_score * nmsParam.imageWidth)
                weightedFace.rectf.bottom = (w_ymax / total_score * nmsParam.imageHeight)
            }

            indexedScore.clear()
//            indexedScore.addAll(remained)
            outputFaces.add(weightedFace)
            Log.d("DEBUG", "outputFaces : ${outputFaces.size}")
            Log.d("DEBUG", "indexedScore : ${indexedScore.size}")

        }

        return outputFaces
    }

    // Computes an overlap similarity between two rectangles. Similarity measure is
    // defined by overlap_type parameter.
    private fun overlapSimilarity(rect1: RectF, rect2: RectF): Float {
        if (!RectF.intersects(rect1, rect2)) return 0.0f

        val intersection = RectF()
        intersection.setIntersect(rect1, rect2)

        val intersectionArea = intersection.height() * intersection.width()

        val normalization = rect1.height() * rect1.width() + rect2.height() * rect2.width() - intersectionArea
        return if (normalization > 0.0f) intersectionArea / normalization else 0.0f
    }
}
