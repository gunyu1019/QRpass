package kr.yhs.qrpass.client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kr.yhs.qrpass.client.listener.FailedResponse
import kr.yhs.qrpass.client.listener.SucceedResponse
import kotlin.coroutines.CoroutineContext

abstract class BaseClient: CoroutineScope, FailedResponse, SucceedResponse {
    private var privateKeyResource: TextView? = null
    private var qrImageResource: ImageView? = null
    var responseStatus: Boolean? = null
    lateinit var responseReason: String

    lateinit var privateKeyResponse: String
    lateinit var qrImageResponse: Any

    private var onSucceedListener: (() -> Unit)? = null
    private var onFailedListener: ((String) -> Unit)? = null

    open val baseLink = ""

    fun setResource(
        privateKey: TextView? = null,
        qrImage: ImageView? = null
    ) {
        privateKeyResource = privateKey
        qrImageResource = qrImage
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

    fun setOnSucceedListener(listener: (() -> Unit)) {
        onSucceedListener = listener
    }

    fun setOnFailedListener(listener: ((String) -> Unit)) {
        onFailedListener = listener
    }

    override fun onSucceed(privateKeyResponse: String, qrImageResponse: Any) {
        this.privateKeyResponse = privateKeyResponse
        this.qrImageResponse = qrImageResponse
        if (privateKeyResource != null)
            privateKeyResource!!.text = privateKeyResponse

        if (qrImageResource != null)
            loadImageWithBase64(
                qrImageResponse as String
            )
        if (onSucceedListener != null)
            onSucceedListener!!.invoke()
    }

    override fun onFailed(responseReason: String) {
        Log.i(TAG, "onFailed(): $responseReason")
        this.responseReason = responseReason
        if (onFailedListener != null)
            onFailedListener!!.invoke(responseReason)
    }

    abstract fun checkBaseLink(url: String): Boolean
    abstract fun getData()

    companion object {
        const val TAG = "Client"
    }

    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main
}
