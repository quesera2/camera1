package sera.sera.que.camera1

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
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

    private val isPortraitMode: Boolean
        get() = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

    init {
        surfaceView.holder.addCallback(Callback())
        addView(surfaceView)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun open() {
        if (camera == null) {
            setupCamera()

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

    private fun setupCamera() {
        Log.i(tag, "open camera")
        val (backCameraId, cameraInfo) = findBackCameraId()
        val camera = Camera.open(backCameraId)
        if (camera != null) {
            val (angle, displayAngle) = getRotation(cameraInfo)
            camera.setDisplayOrientation(displayAngle)
            val params = camera.parameters
            params.setRotation(angle)
            camera.parameters = params
        }
        this.camera = camera
    }

    private fun findBackCameraId(): Pair<Int, CameraInfo> = (0..Camera.getNumberOfCameras())
        .asSequence()
        .map { cameraId -> cameraId to Camera.CameraInfo().also { Camera.getCameraInfo(cameraId, it) } }
        .first { it.second.facing == Camera.CameraInfo.CAMERA_FACING_BACK }

    private fun getRotation(cameraInfo: CameraInfo): Pair<Int, Int> {
        val windowManager = context.getSystemService(WindowManager::class.java)
        val rotation = windowManager.defaultDisplay.rotation
        val degrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> throw RuntimeException("unknown rotation")
        }

        return if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            val angle = (cameraInfo.orientation + degrees) % 360
            val displayAngle = (360 - angle) % 360
            angle to displayAngle
        } else {
            val angle = (cameraInfo.orientation - degrees + 360) % 360
            angle to angle
        }
    }

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