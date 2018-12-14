package info.dvkr.screenstream.data.image

import android.content.Context
import android.graphics.*
import info.dvkr.screenstream.data.R
import info.dvkr.screenstream.data.other.getTag
import info.dvkr.screenstream.data.model.AppError
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import timber.log.Timber


class BitmapNotification(
    context: Context,
    logoBitmap: Bitmap,
    private val outBitmapChannel: SendChannel<Bitmap>,
    onError: (AppError) -> Unit
) : AbstractImageHandler(onError) {

    enum class Type { START, RELOAD_PAGE, NEW_ADDRESS }

    private lateinit var bitmapStart: Bitmap
    private lateinit var bitmapReloadPage: Bitmap
    private lateinit var bitmapNewAddress: Bitmap

    init {
        Timber.tag(getTag("Init")).d("Invoked")

        launch {
            val logo: Bitmap = Bitmap.createScaledBitmap(logoBitmap, 192, 192, false)
            bitmapStart = generateImage(context.getString(R.string.image_generator_press_start), logo)
            bitmapReloadPage = generateImage(context.getString(R.string.image_generator_reload_this_page), logo)
            bitmapNewAddress = generateImage(context.getString(R.string.image_generator_go_to_new_address), logo)
        }
    }

    override fun start() {}

    fun sentBitmapNotification(notificationType: Type) {
        Timber.tag(getTag("sentBitmapNotification")).d("BitmapType: $notificationType")

        val bitmap: Bitmap = when (notificationType) {
            Type.START -> bitmapStart
            Type.RELOAD_PAGE -> bitmapReloadPage
            Type.NEW_ADDRESS -> bitmapNewAddress
        }

        if (outBitmapChannel.isClosedForSend.not()) {
            outBitmapChannel.offer(bitmap)
            outBitmapChannel.offer(bitmap) // Work around bug on some web browsers
            outBitmapChannel.offer(bitmap) // Work around bug on some web browsers
        }
    }

    private fun generateImage(message: String, logo: Bitmap): Bitmap {
        val bitmap: Bitmap = Bitmap.createBitmap(500, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRGB(69, 90, 100)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(logo, 154f, 16f, paint)

        paint.textSize = 20f
        paint.color = Color.WHITE
        val bounds = Rect()
        paint.getTextBounds(message, 0, message.length, bounds)
        val x = (bitmap.width - bounds.width()) / 2f
        canvas.drawText(message, x, 300f, paint)
        return bitmap
    }
}