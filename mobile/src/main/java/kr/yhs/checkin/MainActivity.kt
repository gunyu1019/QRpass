package kr.yhs.checkin

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kr.yhs.checkin.databinding.ActivityMainBinding
import android.view.animation.TranslateAnimation




class MainActivity : AppCompatActivity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager
    private lateinit var dataClient: DataClient

    private var login: Boolean = false

    private fun getCookies(data: String): Map<String, String> {
        val datas = data.split(";")
        val result: MutableMap<String, String> = mutableMapOf()
        for (i in datas) {
            val dataConvert: List<String> = i.split("=")
            if (dataConvert[1] == "")
                continue
            result[dataConvert[0].trim()] = dataConvert[1]
        }
        return result
    }

    private fun inputKey() {
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cookie = CookieManager.getInstance()
        pm = PackageManager("checkIn", this@MainActivity)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webViewLayout.visibility = View.INVISIBLE
        if (supportActionBar != null)
            supportActionBar!!.hide()
        var typeMode = pm.getString("checkMode")
        if (typeMode == null || typeMode == "") {
            pm.setString("checkMode", "na")
            typeMode = "na"
        }

        dataClient = Wearable.WearableOptions.Builder().setLooper(Looper.getMainLooper()).build().let { options ->
            Wearable.getDataClient(this, options)
        }

        if (typeMode == "na") {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")
            if ((pqr == null || aut == null || ses == null) || (pqr == "" || aut == "" || ses == ""))
                login = true

            if (login) {
                slideUp(binding.webViewLayout)
            }
        }
    }

    fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(
            0F,
            0F,
            view.height.toFloat(),
            0F,
        )
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)
    }

    fun slideDown(view: View) {
        val animate = TranslateAnimation(
            0F,
            0F,
            0F,
            view.height.toFloat()
        )
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)
    }
}