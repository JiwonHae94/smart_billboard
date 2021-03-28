package com.nota.nota_android_gs25.camera.support

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.*
import android.media.Image
import android.os.Build
import android.util.Log
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object ImageProxyUtil{
    private val TAG = ImageProxyUtil::class.java.simpleName

    data class FrameMetadata(var width : Int, var height : Int, var rotation : Int)

    @SuppressLint("UnsafeExperimentalUsageError")
    fun ImageProxy.getBitmap() : Bitmap?{
        val frameMetadata = FrameMetadata(this.width, this.height, this.imageInfo.rotationDegrees)
        val nv21Buffer: ByteBuffer =
            yuv420ThreePlanesToNV21(
                this.image!!.planes,
                this.width,
                this.height
            )
        return getBitmap(
            nv21Buffer,
            frameMetadata
        )
    }

    /**
     * Converts YUV_420_888 to NV21 bytebuffer.
     *
     *
     * The NV21 format consists of a single byte array containing the Y, U and V values. For an
     * image of size S, the first S positions of the array contain all the Y values. The remaining
     * positions contain interleaved V and U values. U and V are subsampled by a factor of 2 in both
     * dimensions, so there are S/4 U values and S/4 V values. In summary, the NV21 array will contain
     * S Y values followed by S/4 VU values: YYYYYYYYYYYYYY(...)YVUVUVUVU(...)VU
     *
     *
     * YUV_420_888 is a generic format that can describe any YUV image where U and V are subsampled
     * by a factor of 2 in both dimensions. [Image.getPlanes] returns an array with the Y, U and
     * V planes. The Y plane is guaranteed not to be interleaved, so we can just copy its values into
     * the first part of the NV21 array. The U and V planes may already have the representation in the
     * NV21 format. This happens if the planes share the same buffer, the V buffer is one position
     * before the U buffer and the planes have a pixelStride of 2. If this is case, we can just copy
     * them to the NV21 array.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun yuv420ThreePlanesToNV21(yuv420888planes: Array<Image.Plane>, width: Int, height: Int): ByteBuffer {
        val imageSize = width * height
        val out = ByteArray(imageSize + 2 * (imageSize / 4))
        if (areUVPlanesNV21(yuv420888planes, width, height)) {
            // Copy the Y values.
            yuv420888planes[0].getBuffer().get(out, 0, imageSize)
            val uBuffer: ByteBuffer = yuv420888planes[1].getBuffer()
            val vBuffer: ByteBuffer = yuv420888planes[2].getBuffer()
            // Get the first V value from the V buffer, since the U buffer does not contain it.
            vBuffer[out, imageSize, 1]
            // Copy the first U value and the remaining VU values from the U buffer.
            uBuffer[out, imageSize + 1, 2 * imageSize / 4 - 1]
        } else {
            // Fallback to copying the UV values one by one, which is slower but also works.
            // Unpack Y.
            unpackPlane(yuv420888planes[0], width, height, out, 0, 1)
            // Unpack U.
            unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2)
            // Unpack V.
            unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2)
        }
        return ByteBuffer.wrap(out)
    }

    /**
     * Unpack an image plane into a byte array.
     *
     *
     * The input plane data will be copied in 'out', starting at 'offset' and every pixel will be
     * spaced by 'pixelStride'. Note that there is no row padding on the output.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun unpackPlane(plane: Image.Plane, width: Int, height: Int, out: ByteArray, offset: Int, pixelStride: Int) {
        val buffer: ByteBuffer = plane.getBuffer()
        buffer.rewind()

        // Compute the size of the current plane.
        // We assume that it has the aspect ratio as the original image.
        val numRow: Int = (buffer.limit() + plane.getRowStride() - 1) / plane.getRowStride()
        if (numRow == 0) {
            return
        }
        val scaleFactor = height / numRow
        val numCol = width / scaleFactor

        // Extract the data in the output buffer.
        var outputPos = offset
        var rowStart = 0
        for (row in 0 until numRow) {
            var inputPos = rowStart
            for (col in 0 until numCol) {
                out[outputPos] = buffer[inputPos]
                outputPos += pixelStride
                inputPos += plane.getPixelStride()
            }
            rowStart += plane.getRowStride()
        }
    }

    /** Checks if the UV plane buffers of a YUV_420_888 image are in the NV21 format.  */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun areUVPlanesNV21(planes: Array<Image.Plane>, width: Int, height: Int): Boolean {
        val imageSize = width * height
        val uBuffer: ByteBuffer = planes[1].getBuffer()
        val vBuffer: ByteBuffer = planes[2].getBuffer()

        // Backup buffer properties.
        val vBufferPosition = vBuffer.position()
        val uBufferLimit = uBuffer.limit()

        // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
        vBuffer.position(vBufferPosition + 1)
        // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
        uBuffer.limit(uBufferLimit - 1)

        // Check that the buffers are equal and have the expected number of elements.
        val areNV21 =
            vBuffer.remaining() == 2 * imageSize / 4 - 2 && vBuffer.compareTo(uBuffer) == 0

        // Restore buffers to their initial state.
        vBuffer.position(vBufferPosition)
        uBuffer.limit(uBufferLimit)
        return areNV21
    }


    /** Converts NV21 format byte buffer to bitmap.  */
    @Nullable
    internal fun getBitmap(data: ByteBuffer, metadata: FrameMetadata): Bitmap? {
        data.rewind()
        val imageInBuffer = ByteArray(data.limit())
        data[imageInBuffer, 0, imageInBuffer.size]
        try {
            val image = YuvImage(
                imageInBuffer, ImageFormat.NV21, metadata.width, metadata.height, null
            )
            val stream = ByteArrayOutputStream()
            image.compressToJpeg(Rect(0, 0, metadata.width, metadata.height), 80, stream)
            val bmp: Bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
            stream.close()

            return rotateBitmap(
                bmp,
                metadata.rotation,
                false,
                false
            )
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
        }
        return null
    }

    /** Rotates a bitmap if it is converted from a bytebuffer.  */
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int, flipX: Boolean, flipY: Boolean): Bitmap? {
        val matrix = Matrix()

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees.toFloat())

        // Mirror the image along the X or Y axis.
        matrix.postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap !== bitmap) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }
}