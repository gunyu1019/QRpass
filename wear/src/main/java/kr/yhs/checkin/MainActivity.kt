package kr.yhs.checkin

import android.app.Activity
import android.os.Bundle
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import kr.yhs.checkin.databinding.ActivityMainBinding

class MainActivity : Activity(), DataClient.OnDataChangedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pm = PackageManager("checkIn", this@MainActivity)
    }

    override fun onDataChanged(data: DataEventBuffer) {
        data.forEach{ event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                event.dataItem.also { item ->
                    if (item.uri.path?:"".compareTo("/naverKey") == 0) {
                        DataMapItem.fromDataItem(item).dataMap.apply {
                            val pqr = getString("kr.yhs.checkin.na.NID_PQR")
                            val aut = getString("kr.yhs.checkin.na.NID_AUT")
                            val ses = getString("kr.yhs.checkin.na.NID_SES")
                            var nidNL = false
                            if ((pqr == null || aut == null || ses == null) || (pqr == "" || aut == "" || ses == ""))
                                nidNL = true

                            if (nidNL) {
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