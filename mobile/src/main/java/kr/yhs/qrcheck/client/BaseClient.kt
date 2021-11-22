package kr.yhs.qrcheck.client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kr.yhs.qrcheck.MainActivity
import kotlin.coroutines.CoroutineContext

abstract class BaseClient(open val activity: MainActivity): CoroutineScope {
    private var privateKeyResource: TextView? = null
    private var qrImageResource: ImageView? = null
    private var webViewResource: WebView? = null
    var responseStatus: Boolean? = null
    lateinit var responseReason: String

    lateinit var privateKeyResponse: String
    lateinit var qrImageResponse: Any

    open val baseLink = ""

    fun setResource(
        privateKey: TextView? = null,
        qrImage: ImageView? = null,
        webView: WebView? = null
    ) {
        privateKeyResource = privateKey
        qrImageResource = qrImage
        webViewResource = webView
    }

    open fun <T> onLoad(vararg args: T) {
        mJob = Job()
    }

    private fun loadImageWithBase64(resource: String = qrImageResponse as String) {
        val base64Image: String = resource.split(",")[1]
        val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        if (qrImageResponse != resource)
            qrImageResponse = resource

        qrImageResource!!.setImageBitmap(bitmap)
    }

    fun updateResource() {
        if (privateKeyResource != null)
            privateKeyResource!!.text = privateKeyResponse

        if (qrImageResource != null)
            loadImageWithBase64(
                qrImageResponse as String
            )
    }

    fun failedResource() {
    }

    abstract fun checkBaseLink(url: String): Boolean
    abstract fun getData()

    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main
}