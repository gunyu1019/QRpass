package kr.yhs.checkin

import android.app.Activity
import android.os.Looper
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable

class WearableActivity(context: Activity) {
    private var pm = PackageManager("checkIn", context)
    private var dataClient = Wearable.WearableOptions.Builder().setLooper(Looper.getMainLooper()).build().let { options ->
        Wearable.getDataClient(context, options)
    }

    fun inputKey() {
        if (pm.getString("checkMode") == "na") {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")
            val putDataReq: PutDataRequest = PutDataMapRequest.create("/naKey").run {
                dataMap.putString("kr.yhs.checkin.na.NID_PQR", pqr ?: "")
                dataMap.putString("kr.yhs.checkin.na.NID_AUT", aut ?: "")
                dataMap.putString("kr.yhs.checkin.na.NID_SES", ses ?: "")
                asPutDataRequest()
            }
            dataClient.putDataItem(putDataReq)
        }
        return
    }
}