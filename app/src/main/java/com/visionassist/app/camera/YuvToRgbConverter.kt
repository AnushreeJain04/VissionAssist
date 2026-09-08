package com.visionassist.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import androidx.camera.core.ImageProxy

/**
 * Converts a YUV_420_888 camera frame (the raw format CameraX delivers
 * during continuous analysis) into a standard Bitmap for ML processing.
 */
class YuvToRgbConverter(context: Context) {

    private val rs = RenderScript.create(context)
    private val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    private var yuvBits: ByteArray? = null
    private var inputAllocation: Allocation? = null
    private var outputAllocation: Allocation? = null

    @Synchronized
    fun yuvToRgb(image: ImageProxy, output: Bitmap) {
        val yuvBytes = imageProxyToNv21(image)

        if (inputAllocation == null) {
            val yuvType = Type.Builder(rs, Element.U8(rs))
                .setX(yuvBytes.size)
            inputAllocation = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)

            val rgbType = Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(image.width)
                .setY(image.height)
            outputAllocation = Allocation.createTyped(rs, rgbType.create(), Allocation.USAGE_SCRIPT)
        }

        inputAllocation?.copyFrom(yuvBytes)
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation?.copyTo(output)
    }

    private fun imageProxyToNv21(image: ImageProxy): ByteArray {
        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer

        val ySize = yPlane.remaining()
        val uSize = uPlane.remaining()
        val vSize = vPlane.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yPlane.get(nv21, 0, ySize)
        vPlane.get(nv21, ySize, vSize)
        uPlane.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}