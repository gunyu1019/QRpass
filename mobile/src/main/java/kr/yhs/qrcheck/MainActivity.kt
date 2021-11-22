package kr.yhs.qrcheck

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.TranslateAnimation
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.android.material.navigation.NavigationBarView
import kr.yhs.checkin.PackageManager
import kr.yhs.checkin.WearableManager
import kr.yhs.qrcheck.client.BaseClient
import kr.yhs.qrcheck.client.NaverClient
import kr.yhs.qrcheck.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager
    private var wearClient = WearableManager()

    private var loginRequired: Boolean = false
    private var wearClientDetected: Boolean = false
    private var typeClient: Int = 0

    private var infoSlideView: Boolean = false
    private var logoutSlideView: Boolean = false

    private lateinit var client: BaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pm = PackageManager("QRpass", this@MainActivity)
        wearClient.loadClient(this@MainActivity)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webViewLayout.visibility = View.INVISIBLE
        binding.infoLayout.visibility = View.INVISIBLE
        binding.logoutLayout.visibility = View.INVISIBLE
        if (supportActionBar != null)
            supportActionBar!!.hide()
        typeClient = pm.getInt("clientMode", -1)
        when (typeClient) {
            -1 -> {
                client = NaverClient(this@MainActivity)
                pm.setInt("clientMode", 0)
            }
            0 -> {
                client = NaverClient(this@MainActivity)
            }
        }
        client.setResource(
            binding.privateCode,
            binding.qrImage,
            binding.webViewFrame
        )

        // binding setting listener
        binding.bottomNavigationView.setOnItemSelectedListener(this)
        binding.refreshBtn.setOnClickListener {
            mainProcess()
        }
        binding.logoutSuccessBtn.setOnClickListener {
            if (logoutSlideView) {
                slideDown(binding.logoutLayout)
                logoutSlideView = false
            }
            val cookie = CookieManager.getInstance()
            cookie.removeAllCookies(null)
            cookie.flush()
            if (typeClient == 0) {
                loginRequired = true
                pm.removeKey("NID_PQR")
                pm.removeKey("NID_AUT")
                pm.removeKey("NID_SES")
            }
            webViewProcess("로그인이 필요합니다.")
        }
        binding.logoutCancelBtn.setOnClickListener {
            if (logoutSlideView) {
                slideDown(binding.logoutLayout)
                logoutSlideView = false
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

        if (typeClient == 0) {
            val pqr = pm.getString("NID_PQR")?: ""
            val aut = pm.getString("NID_AUT")?: ""
            val ses = pm.getString("NID_SES")?: ""
            if (pqr == "" || aut == "" || ses == "")
                loginRequired = true

            if (loginRequired) {
                webViewProcess()
            } else {
                mainProcess()
            }
        }
    }

    private fun mainProcess() {
        if (typeClient == 0) {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")
            client.onLoad(pqr, aut, ses)
        }
        client.getData()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun webViewProcess(comment: String? = null) {
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
                        url == client.baseLink -> {
                            val data = getCookies(
                                cookie.getCookie(client.baseLink)
                            )
                            loginRequired = false

                            if (typeClient == 0) {
                                pm.setString("NID_PQR", data["NID_PQR"] ?: "")
                                pm.setString("NID_AUT", data["NID_AUT"] ?: "")
                                pm.setString("NID_SES", data["NID_SES"] ?: "")

                                wearClient.putData(
                                    wearClient.NAVER_TOKEN,
                                    mapOf(
                                        "kr.yhs.checkin.token.NID_PQR" to (data["NID_PQR"] ?: ""),
                                        "kr.yhs.checkin.token.NID_AUT" to (data["NID_AUT"] ?: ""),
                                        "kr.yhs.checkin.token.NID_SES" to (data["NID_SES"] ?: "")
                                    )
                                )
                                loadUrl("https://m.naver.com")
                            }
                            slideDown(binding.webViewLayout)
                            mainProcess()
                        }
                        client.checkBaseLink(url?:"") -> {
                            loginRequired = true
                        }
                    }
                }
            }
            webViewClient.apply {
                loadUrl(client.baseLink)
            }
        }
    }

    private fun getCookies(data: String?): Map<String, String> {
        if (data == null)
            return mapOf()

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
        view.visibility = View.GONE
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.log_inout -> {
                logoutSlideView = if (logoutSlideView) {
                    slideDown(binding.logoutLayout)
                    false
                } else {
                    slideUp(binding.logoutLayout)
                    true
                }
            }
            R.id.information -> {
                infoSlideView = if (infoSlideView) {
                    slideDown(binding.infoLayout)
                    false
                } else {
                    slideUp(binding.infoLayout)
                    true
                }
            }
            R.id.refresh_wearable -> {
                val pqr = pm.getString("NID_PQR")
                val aut = pm.getString("NID_AUT")
                val ses = pm.getString("NID_SES")
                wearClient.putData(
                    wearClient.NAVER_TOKEN,
                    mapOf(
                        "kr.yhs.checkin.token.NID_PQR" to (pqr ?: ""),
                        "kr.yhs.checkin.token.NID_AUT" to (aut ?: ""),
                        "kr.yhs.checkin.token.NID_SES" to (ses ?: "")
                    ),
                    successListener = {
                        Log.i("WearableClient[Listener]", "Success send data to Wearable")
                    }
                )
            }
        }
        return false
    }

    companion object {
    }
}