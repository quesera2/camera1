package sera.sera.que.camera1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var preview: CameraPreview

    private val requestCamera = 1

    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preview = findViewById(R.id.preview)
        findViewById<FloatingActionButton>(R.id.button_picture).apply {
            setOnClickListener { preview.takePicture(this@MainActivity::onTakePicture) }
        }

        checkPermission()
    }

    override fun onStop() {
        super.onStop()
        preview.stop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCamera) {
            assert(permissions.first() == Manifest.permission.CAMERA)
            if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            }
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), requestCamera)
        }
    }

    private fun startCamera() {
        Log.i(tag, "call start camera")
        assert(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        preview.open()
    }

    private fun onTakePicture(data: ByteArray) {
        Log.i(tag, "size: ${Formatter.formatFileSize(this, data.size.toLong())}")
    }
}
