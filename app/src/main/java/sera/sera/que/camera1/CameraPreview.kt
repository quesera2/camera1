package sera.sera.que.camera1

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import kotlin.math.max


@Suppress("deprecation")
class CameraPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), Camera.ShutterCallback {

    private val tag = "CameraPreview"

    private val root: ConstraintLayout
    private val shutter: View
    private val surfaceView: SurfaceView

    private var camera: Camera? = null

    private var startRequested: Boolean = false
    private var surfaceAvailable: Boolean = false

    private var executeAutoFocus: Boolean = false

    private val isPortraitMode: Boolean
        get() = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

    init {
        LayoutInflater.from(context).inflate(R.layout.view_camera_preview, this, true)

        root = findViewById(R.id.root)
        shutter = findViewById(R.id.shutter)
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

    @Synchronized
    fun takePicture(handler: ((ByteArray) -> Unit)) {
        val camera = camera ?: throw RuntimeException("take picture without setup camera.")
        // 多重に AutoFocus を実行しない
        if (executeAutoFocus) return

        val doTakePicture = { outerCamera: Camera ->
            onShutter()
            outerCamera.setOneShotPreviewCallback { data, innerCamera ->
                val previewSize = innerCamera.parameters.previewSize
                val (width, height) = previewSize.width to previewSize.height

                val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
                val stream = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, width, height), 50, stream)
                val jpeg = stream.toByteArray()
                handler(jpeg)
            }
        }

        val supportAutoFocus = camera.parameters
            .supportedFocusModes
            .contains(Camera.Parameters.FOCUS_MODE_AUTO)

        if (supportAutoFocus) {
            executeAutoFocus = true
            camera.autoFocus { _, innerCamera ->
                doTakePicture(innerCamera)
                executeAutoFocus = false
            }
        } else {
            doTakePicture(camera)
        }
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
            val previewSize = getDesiredPreviewSize(camera, 1500)
            params.setPreviewSize(previewSize.width, previewSize.height)
            camera.parameters = params
            setupConstraint(previewSize)
        }
        this.camera = camera
    }

    private fun getDesiredPreviewSize(camera: Camera, requiredSize: Int): Camera.Size {
        return camera.parameters
            .supportedPreviewSizes
            .mapNotNull {
                val shortSide = max(it.width, it.height)
                if (shortSide <= requiredSize) shortSide to it else null
            }
            .maxBy { it.first }
            ?.second!!
    }

    private fun setupConstraint(previewSize: Camera.Size) {
        val set = ConstraintSet()
        set.clone(root)

        val (width, height) = if (isPortraitMode) {
            previewSize.height to previewSize.width
        } else {
            previewSize.width to previewSize.height
        }

        Log.d(tag, "constraint: $width:$height")
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

    override fun onShutter() {
        val white = ContextCompat.getColor(context, android.R.color.white)
//        val black = ContextCompat.getColor(context, android.R.color.black)
        val transparent = ContextCompat.getColor(context, android.R.color.transparent)

        val flash = ValueAnimator.ofObject(ArgbEvaluator(), transparent, white).apply {
            duration = 100
            repeatCount = 1
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator -> shutter.setBackgroundColor(animator.animatedValue as Int) }
        }
//        val fadeOut = ValueAnimator.ofObject(ArgbEvaluator(), transparent, black).apply {
//            duration = 200
//            startDelay = 800
//            addUpdateListener { animator -> shutter.setBackgroundColor(animator.animatedValue as Int) }
//        }
        AnimatorSet().apply {
            playTogether(
                flash
//                ,fadeOut
            )
            start()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        startIfReady()
    }

    private inner class Callback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder?) {
            surfaceAvailable = true
            startIfReady()
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            surfaceAvailable = false
        }
    }
}