package sera.sera.que.camera1

import android.Manifest
import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.annotation.RequiresPermission


@Suppress("deprecation")
class CameraPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val tag = "CameraPreview"

    private val surfaceView: SurfaceView = SurfaceView(context)

    private var camera: Camera? = null

    private var startRequested: Boolean = false
    private var surfaceAvailable: Boolean = false

    init {
        surfaceView.holder.addCallback(Callback())
        addView(surfaceView)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun open() {
        if (camera == null) {
            Log.i(tag, "open camera")
            val backCameraId = findBackCameraId()
            camera = Camera.open(backCameraId)

            startRequested = true
            startIfReady()
        }
    }

    fun stop() {
        Log.i(tag, "stop preview")

        // TODO
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun startIfReady() {
        val camera = camera ?: return
        if (startRequested && surfaceAvailable) {
            Log.i(tag, "start preview")

            camera.setPreviewDisplay(surfaceView.holder)
            camera.startPreview()

            startRequested = false
        }
    }

    private fun findBackCameraId(): Int = (0..Camera.getNumberOfCameras())
        .asSequence()
        .map { cameraId -> cameraId to Camera.CameraInfo().also { Camera.getCameraInfo(cameraId, it) } }
        .first { it.second.facing == Camera.CameraInfo.CAMERA_FACING_BACK }
        .first

    private inner class Callback : SurfaceHolder.Callback {
        override fun surfaceCreated(surface: SurfaceHolder?) {
            surfaceAvailable = true
            startIfReady()
        }

        override fun surfaceChanged(surface: SurfaceHolder?, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(surface: SurfaceHolder?) {
        }
    }
}