package kr.yhs.checkin

import android.app.Activity
import android.os.Looper
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

import com.google.android.gms.wearable.DataItem
import java.util.concurrent.ExecutionException


class WearableActivity(context: Activity) {
    private var pm = PackageManager("checkIn", context)
    private var dataClient = Wearable.WearableOptions.Builder().setLooper(Looper.getMainLooper()).build().let { options ->
        Wearable.getDataClient(context, options)
    }

    fun inputKey(type: String? = null) {
        if (type?:(pm.getString("checkMode")?:"") == "na") {
            Log.i("Wearable-inputData", "$type")
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")

            val putDataMapRequest = PutDataMapRequest.create("/naKey")
            val dataMap = putDataMapRequest.dataMap
            dataMap.putString("kr.yhs.checkin.NID_PQR", pqr ?: "")
            dataMap.putString("kr.yhs.checkin.NID_AUT", aut ?: "")
            dataMap.putString("kr.yhs.checkin.NID_SES", ses ?: "")
            val request: PutDataRequest = putDataMapRequest.asPutDataRequest()
            request.setUrgent()
            dataClient.putDataItem(request).apply {
                addOnSuccessListener {
                    Log.i("Wearable-inputData", "Message sent: $it")
                }
                addOnFailureListener {
                    Log.i("Wearable-inputData", "Message NOT sent, error: $it")
                }
            }
            /* val putDataReq: PutDataRequest = PutDataMapRequest.create("/naKey").run {
                dataMap.putString("kr.yhs.checkin.NID_PQR", pqr ?: "")
                dataMap.putString("kr.yhs.checkin.NID_AUT", aut ?: "")
                dataMap.putString("kr.yhs.checkin.NID_SES", ses ?: "")
                asPutDataRequest()
            }
            dataClient.putDataItem(putDataReq).apply {
                addOnSuccessListener {
                    Log.i("Wearable-inputData", "Message sent: ${it}")
                }
                addOnFailureListener {
                    Log.i("Wearable-inputData", "Message NOT sent, error: $it")
                }
            } */
        }
        return
    }
}