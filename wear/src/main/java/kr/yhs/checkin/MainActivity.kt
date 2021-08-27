package kr.yhs.checkin

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.wearable.*
import kr.yhs.checkin.databinding.ActivityMainBinding

class MainActivity : Activity(), DataClient.OnDataChangedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pm = PackageManager("checkIn", this@MainActivity)
        val pqr = pm.getString("NID_PQR")
        val aut = pm.getString("NID_AUT")
        val ses = pm.getString("NID_SES")

        Log.d("cookie", "NID_PQR=${pqr};NID_AUT=${aut};NID_SES=${ses};")
    }

    override fun onDataChanged(data: DataEventBuffer) {
        data.forEach{ event ->
            Log.d("Wearable-inputData", "data-received ${event.type}")
            if (event.type == DataEvent.TYPE_CHANGED) {
                event.dataItem.also { item ->
                    Log.d("Wearable-inputData", "item-url ${item.uri.path}")
                    if (item.uri.path == "/naKey") {
                        DataMapItem.fromDataItem(item).dataMap.apply {
                            val pqr = getString("kr.yhs.checkin.na.NID_PQR")
                            val aut = getString("kr.yhs.checkin.na.NID_AUT")
                            val ses = getString("kr.yhs.checkin.na.NID_SES")
                            var nidNL = false
                            if ((pqr == null || aut == null || ses == null) || (pqr == "" || aut == "" || ses == ""))
                                nidNL = true

                            if (!nidNL) {
                                pm.setString("NID_PQR", pqr)
                                pm.setString("NID_AUT", aut)
                                pm.setString("NID_SES", ses)
                            }
                        }
                    }
                }
            }
        }
    }
}