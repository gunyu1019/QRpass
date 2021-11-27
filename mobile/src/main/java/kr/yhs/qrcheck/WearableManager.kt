package kr.yhs.qrcheck


import android.os.Looper
import android.util.Log
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kr.yhs.qrcheck.MainActivity
import kotlin.coroutines.CoroutineContext


class WearableManager {
    private lateinit var pm: PackageManager
    private lateinit var dataClient: DataClient
    val naverToken = "/naver/token"

    fun loadClient(context: MainActivity) {
        pm = PackageManager("QRpass", context)
        dataClient = Wearable.getDataClient(context)
    }

    fun insertData(
        key: String,
        data: Map<String, Any>,
        successListener: OnSuccessListener<Any>? = null,
        failureListener: OnFailureListener? = null
    ) {
        Log.i("$TAG [key]", key)
        Log.i("$TAG [value]", data.toString())
        val putDataReq: PutDataRequest = PutDataMapRequest.create(key).run {
            data.forEach { (key, value) ->
                Log.d("$TAG [data-forEach]", "$key: $value")
                when (value) {
                    is String -> dataMap.putString(key, value)
                    is Int -> dataMap.putInt(key, value)
                    is Boolean -> dataMap.putBoolean(key, value)
                    is Byte -> dataMap.putByte(key, value)
                    is Double -> dataMap.putDouble(key, value)
                    is Float -> dataMap.putFloat(key, value)
                }
            }
            asPutDataRequest().setUrgent()
        }
        dataClient.putDataItem(putDataReq).apply {
            if (successListener != null) {
                addOnSuccessListener(successListener)
            }
            if (failureListener != null) {
                addOnFailureListener(failureListener)
            }
        }
    }

    companion object {
        private const val TAG = "WearableManager"
        const val CAPABILITY_WEAR_APP = "qrpass_wear_app"
    }
}