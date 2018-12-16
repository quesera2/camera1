package sera.sera.que.camera1

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import kotlin.math.max


@Suppress("deprecation")
class CameraPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    private val tag = "CameraPreview"

    private val root: ConstraintLayout
    private val surfaceView: SurfaceView

    private var camera: Camera? = null

    private var startRequested: Boolean = false
    private var surfaceAvailable: Boolean = false

    private val isPortraitMode: Boolean
        get() = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

    init {
        LayoutInflater.from(context).inflate(R.layout.view_camera_preview, this, true)

        root = findViewById(R.id.root)
        surfaceView = findViewById(R.id.surface)
        surfaceView.holder.addCallback(Callback())
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

        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    fun takePicture(handler: ((ByteArray) -> Unit)) {
        val camera = camera ?: throw RuntimeException("take picture without setup camera.")

        camera.takePicture(null, null, null,
            Camera.PictureCallback { bytes, _ ->
                handler(bytes)
                // restart preview if needed
                // camera.startPreview()
            })
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
            if (params.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                Log.d(tag, "support continuous auto focus.")
                params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }
            params.jpegQuality = 100
            camera.parameters = params
            desiredPictureSize(camera)
            setupConstraint()
        }
        this.camera = camera
    }

    private fun setupConstraint() {
        val set = ConstraintSet()
        set.clone(root)

        val previewSize = camera?.parameters?.previewSize
        val (width, height) = if (previewSize != null) {
            Log.d(tag, "previewSize ${previewSize.width},${previewSize.height}")
            previewSize.width.toFloat() to previewSize.height.toFloat()
        } else {
            320.0f to 240.0f
        }.let { (width, height) ->
            // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
            if (isPortraitMode) height to width else width to height
        }

        set.setDimensionRatio(surfaceView.id, "$width:$height")
        set.applyTo(root)
    }

    private fun findBackCameraId(): Pair<Int, CameraInfo> = (0..Camera.getNumberOfCameras())
        .asSequence()
        .map { cameraId -> cameraId to Camera.CameraInfo().also { Camera.getCameraInfo(cameraId, it) } }
        .first { it.second.facing == Camera.CameraInfo.CAMERA_FACING_BACK }

    private fun getRotation(cameraInfo: CameraInfo): Pair<Int, Int> {
        val windowManager = ContextCompat.getSystemService(context, WindowManager::class.java)
            ?: throw RuntimeException("window manager not found.")
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

    private fun desiredPictureSize(camera: Camera) {
        // 長辺が requireSize より短い PictureSize を探す
        val requiredSize = 1500
        val parameters = camera.parameters
        val size = parameters
            .supportedPictureSizes
            .mapNotNull {
                val shortSide = max(it.width, it.height)
                if (shortSide <= requiredSize) shortSide to it else null
            }
            .maxBy { it.first }
            ?.second

        if (size != null) {
            parameters.setPictureSize(size.width, size.height)
            Log.d(tag, "picture size ${size.width}, ${size.height}")
        }
    }

    private inner class Callback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder?) {
            surfaceAvailable = true
            startIfReady()
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
        }
    }
}