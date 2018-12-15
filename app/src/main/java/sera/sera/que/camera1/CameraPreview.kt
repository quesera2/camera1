package sera.sera.que.camera1

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout

class CameraPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val surfaceView: SurfaceView = SurfaceView(context).apply {
        holder.addCallback(Callback())
        addView(this)
    }

    private inner class Callback : SurfaceHolder.Callback {
        override fun surfaceCreated(surface: SurfaceHolder?) {
        }

        override fun surfaceChanged(surface: SurfaceHolder?, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(surface: SurfaceHolder?) {
        }
    }
}