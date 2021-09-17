package kr.yhs.checkin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.view.animation.TranslateAnimation
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationBarView
import kr.yhs.checkin.databinding.ActivityMainBinding
import org.jsoup.Jsoup
import kotlin.concurrent.timer


class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager
    private lateinit var wa: WearableActivity

    private var login: Boolean = false
    private var infoSlide: Boolean = false
    private var logoutSlide: Boolean = false
    private lateinit var typeMode: String

    private lateinit var privacyNumber: String

    private val naverLink = "https://nid.naver.com/login/privacyQR"

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

            Thread {
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

                        this@MainActivity.runOnUiThread {
                            loadImage(base64.toString())
                            binding.privateCode.text = privacyNumber
                            binding.refreshBtn.visibility = View.GONE
                            binding.timerCount.text = getString(R.string.count, 15)

                            var second = 0
                            timer(period = 1000, initialDelay = 1000) {
                                this@MainActivity.runOnUiThread {
                                    binding.timerCount.text = getString(R.string.count, 15 - second)
                                }
                                second++
                                if (second == 15) {
                                    this@MainActivity.runOnUiThread {
                                        binding.refreshBtn.visibility = View.VISIBLE
                                    }
                                    cancel()
                                } else if (login) {
                                    cancel()
                                }
                            }
                        }
                    } else if (wrap.select(".self_box").html() != "") {
                        this@MainActivity.runOnUiThread {
                            binding.refreshBtn.visibility = View.GONE
                            processLogin(getString(R.string.need_authorize))
                        }
                    }
                } else if (html.select(".login_wrap").html() != "") {
                    this@MainActivity.runOnUiThread {
                        binding.refreshBtn.visibility = View.GONE
                        processLogin(getString(R.string.login_expired))
                    }
                }
            }.start()
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
                            login = false
                            slideDown(binding.webViewLayout)
                            loadUrl("https://m.naver.com")
                            wa.inputKey()
                            processMain()
                        }
                        url ?: "".indexOf("https://nid.naver.com/nidlogin.login") == 0 -> {
                            login = true
                        }
                        url ?: "".indexOf("https://nid.naver.com/iasystem/mobile_pop.nhn") == 0 -> {
                            login = true
                        }
                    }
                }
            }
            webViewClient.apply {
                if (typeMode == "na")
                    loadUrl(naverLink)
            }
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pm = PackageManager("checkIn", this@MainActivity)
        wa = WearableActivity(this@MainActivity)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webViewLayout.visibility = View.INVISIBLE
        binding.infoLayout.visibility = View.INVISIBLE
        binding.logoutLayout.visibility = View.INVISIBLE
        if (supportActionBar != null)
            supportActionBar!!.hide()
        typeMode = pm.getString("checkMode")?: ""
        if (typeMode == "") {
            pm.setString("checkMode", "na")
            typeMode = "na"
        }

        binding.bottomNavigationView.setOnItemSelectedListener(this)
        binding.refreshBtn.setOnClickListener {
            processMain()
        }
        binding.logoutSuccessBtn.setOnClickListener {
            if (logoutSlide) {
                slideDown(binding.logoutLayout)
                logoutSlide = false
            }
            val cookie = CookieManager.getInstance()
            cookie.removeAllCookies(null)
            cookie.flush()
            if (pm.getString("checkMode") == "na") {
                login = true
                pm.removeKey("NID_PQR")
                pm.removeKey("NID_AUT")
                pm.removeKey("NID_SES")
            }
            processLogin("로그인이 필요합니다.")
        }
        binding.logoutCancelBtn.setOnClickListener {
            if (logoutSlide) {
                slideDown(binding.logoutLayout)
                logoutSlide = false
            }
        }
        binding.soruceCodeButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gunyu1019/QRpass"))
            startActivity(intent)
        }
        binding.websiteButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yhs.kr"))
            startActivity(intent)
        }
        binding.forumButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yhs.kr/YBOT/forum.html"))
            startActivity(intent)
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
                wa.inputKey()
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
        view.visibility = View.INVISIBLE
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.log_inout -> {
                logoutSlide = if (logoutSlide) {
                    slideDown(binding.logoutLayout)
                    false
                } else {
                    slideUp(binding.logoutLayout)
                    true
                }
            }
            R.id.information -> {
                infoSlide = if (infoSlide) {
                    slideDown(binding.infoLayout)
                    false
                } else {
                    slideUp(binding.infoLayout)
                    true
                }
            }
            R.id.refresh_wearable -> {
                wa.inputKey()
            }
        }
        return false
    }

    override fun onBackPressed() {
        when {
            infoSlide -> {
                slideDown(binding.infoLayout)
                infoSlide = false
            }
            logoutSlide -> {
                slideDown(binding.logoutLayout)
                logoutSlide = false
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}