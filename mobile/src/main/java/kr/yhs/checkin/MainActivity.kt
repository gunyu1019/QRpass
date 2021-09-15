package kr.yhs.checkin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kr.yhs.checkin.databinding.ActivityMainBinding
import android.view.animation.TranslateAnimation
import android.webkit.WebView
import android.webkit.WebViewClient
import org.jsoup.Jsoup
import kotlin.concurrent.timer


class MainActivity : AppCompatActivity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager
    private lateinit var dataClient: DataClient

    private var login: Boolean = false
    private lateinit var typeMode: String
    private lateinit var privacyNumber: String

    private val naverLink = "https://nid.naver.com/login/privacyQR"

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

    private fun loadImage(base64: String) {
        val base64Image: String = base64.split(",")[1]
        val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        binding.mainQR.setImageBitmap(bitmap)
    }

    private fun processMain() {
        if (typeMode == "na") {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")
            Thread (
                Runnable {
                    val response = Jsoup.connect(naverLink)
                        .header("Cookie", "NID_PQR=${pqr};NID_AUT=${aut};NID_SES=${ses};")
                        .get()
                    val html = response.body()
                    if (html.select("div.qr_wrap").html() != "") {
                        val wrap = html.select("div.qr_wrap")
                        if (wrap.select("div.qr_area").html() != "") {
                            val area = wrap.select("div.qr_area")
                            val base64 = area.select("div.qr_box img").attr("src")
                            val numberHTML = area.select("div.number_box span.number")
                            privacyNumber = numberHTML.text()

                            this@MainActivity.runOnUiThread(java.lang.Runnable {
                                loadImage(base64.toString())
                                binding.privateCode.text = privacyNumber
                                binding.refreshBtn.visibility = View.GONE
                                binding.timerCount.text = getString(R.string.count, 15)

                                var second = 0
                                timer(period = 1000, initialDelay = 1000) {
                                    runOnUiThread {
                                        binding.timerCount.text = getString(R.string.count, 15 - second)
                                    }
                                    second++
                                    if (second == 15) {
                                        runOnUiThread {
                                            binding.refreshBtn.visibility = View.VISIBLE
                                        }
                                        cancel()
                                    }
                                }
                                return@Runnable
                            })
                        } else if (wrap.select(".self_box").html() != "") {
                            this@MainActivity.runOnUiThread(java.lang.Runnable {
                                binding.refreshBtn.visibility = View.GONE
                                processLogin(getString(R.string.need_authorize))
                                return@Runnable
                            })
                        }
                    } else if (html.select(".login_wrap").html() != "") {
                        this@MainActivity.runOnUiThread(java.lang.Runnable {
                            binding.refreshBtn.visibility = View.GONE
                            processLogin(getString(R.string.login_expired))
                            return@Runnable
                        })
                    }
                }
            ).start()
        }
    }

    private fun processLogin(comment: String? = null) {
        val cookie = CookieManager.getInstance()
        slideUp(binding.webViewLayout)
        if (comment == null) {
            binding.warningMessage.visibility = View.INVISIBLE
        } else {
            binding.warningMessage.text = comment
        }

        binding.webViewFrame.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowContentAccess = true
                allowFileAccess = true
                setSupportMultipleWindows(true)
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    when {
                        url == naverLink -> {
                            val data = getCookies(
                                cookie.getCookie(naverLink)
                            )

                            pm.setString("NID_PQR", data["NID_PQR"] ?: "")
                            pm.setString("NID_AUT", data["NID_AUT"] ?: "")
                            pm.setString("NID_SES", data["NID_SES"] ?: "")
                            inputKey()
                            slideDown(binding.webViewLayout)
                            processMain()
                            return
                        }
                        url ?: "".indexOf("https://nid.naver.com/nidlogin.login") == 0 -> {
                            login = false
                        }
                        url ?: "".indexOf("https://nid.naver.com/iasystem/mobile_pop.nhn") == 0 -> {
                            login = false
                        }
                    }
                }
            }
            webViewClient.apply {
                loadUrl(naverLink)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pm = PackageManager("checkIn", this@MainActivity)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webViewLayout.visibility = View.INVISIBLE
        if (supportActionBar != null)
            supportActionBar!!.hide()
        typeMode = pm.getString("checkMode")?: ""
        if (typeMode == "") {
            pm.setString("checkMode", "na")
            typeMode = "na"
        }

        binding.refreshBtn.setOnClickListener {
            processMain()
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
                processLogin()
            } else {
                inputKey()
                processMain()
            }
        }
    }

    private fun slideUp(view: View) {
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

    private fun slideDown(view: View) {
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