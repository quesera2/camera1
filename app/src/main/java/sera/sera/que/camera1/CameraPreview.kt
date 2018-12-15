package sera.sera.que.camera1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

@Suppress("deprecation")
class CameraPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), LifecycleObserver {

    private val tag = "CameraPreview"

    private val surfaceView: SurfaceView = SurfaceView(context)

    private var camera: Camera? = null

    init {
        surfaceView.holder.addCallback(Callback())
        addView(surfaceView)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun open() {
        Log.i(tag, "open camera")
        if (camera == null) {
            val backCameraId = findBackCameraId()
            camera = Camera.open(backCameraId)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun startPreview() {
        Log.i(tag, "start preview")

        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (camera == null
            && permission == PackageManager.PERMISSION_GRANTED
        ) {
            open()
        }

        // TODO
        camera?.let { camera ->
            camera.setPreviewDisplay(surfaceView.holder)
            camera.startPreview()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun stopPreview() {
        Log.i(tag, "stop preview")

        // TODO
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun findBackCameraId(): Int = (0..Camera.getNumberOfCameras())
        .asSequence()
        .map { cameraId -> cameraId to Camera.CameraInfo().also { Camera.getCameraInfo(cameraId, it) } }
        .first { it.second.facing == Camera.CameraInfo.CAMERA_FACING_BACK }
        .first

    private inner class Callback : SurfaceHolder.Callback {
        override fun surfaceCreated(surface: SurfaceHolder?) {
        }

        override fun surfaceChanged(surface: SurfaceHolder?, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(surface: SurfaceHolder?) {
        }
    }
}