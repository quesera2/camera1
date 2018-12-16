package sera.sera.que.camera1

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView

private const val BUNDLE_KEY_IMAGE = "bundle-key-image"

fun imageActivityIntentOf(
    context: Context,
    byteArray: ByteArray
) = Intent(context, ImageActivity::class.java).apply {
    putExtra(BUNDLE_KEY_IMAGE, byteArray)
}

class ImageActivity : AppCompatActivity() {

    private lateinit var imageView: AppCompatImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        imageView = findViewById(R.id.image)

        val byteArray = intent.getByteArrayExtra(BUNDLE_KEY_IMAGE)
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        imageView.setImageBitmap(bitmap)
    }
}
