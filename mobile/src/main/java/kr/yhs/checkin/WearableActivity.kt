package kr.yhs.checkin

import android.app.Activity
import android.os.Looper
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable


class WearableActivity {
    private lateinit var pm: PackageManager
    private lateinit var dataClient: DataClient

    val NAVER_TOKEN = "/naver/token"

    fun loadClient(context: Activity) {
        pm = PackageManager("checkIn", context)
        dataClient = Wearable.WearableOptions.Builder().setLooper(
            Looper.getMainLooper()
        ).build().let { options ->
            Wearable.getDataClient(context, options)
        }
    }

    fun putData(
        key: String,
        data: Map<String, Any>,
        successListener: OnSuccessListener<Any>? = null,
        failureListener: OnFailureListener? = null
    ) {
        Log.i("WearableClient [key]", key)
        Log.i("WearableClient [value]", data.toString())
        val putDataReq: PutDataRequest = PutDataMapRequest.create(key).run {
            data.forEach { (key, value) ->
                Log.d("WearableClient [data-forEach]", "$key: $value")
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
}