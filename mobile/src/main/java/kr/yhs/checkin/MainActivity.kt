package kr.yhs.checkin

import android.os.Bundle
import android.view.MenuItem
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationView
import kr.yhs.checkin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private val pm = PackageManager("checkIn", this@MainActivity)
    private val naQRBase = "https://nid.naver.com/login/privacyQR"

    private fun getCookies(data: String): Map<String, String> {
        val datas = data.split(";")
        lateinit var result: MutableMap<String, String>
        for (i in datas) {
            val dataConvert = data.split("=")
            result[dataConvert[0]] = dataConvert[1]
        }
        return result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cookie = CookieManager.getInstance()
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (supportActionBar != null)
            supportActionBar!!.hide()
        var typeMode = pm.getString("checkMode")
        if (typeMode == null || typeMode == "") {
            pm.setString("checkMode", "na")
            typeMode = "na"
        }

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            allowFileAccess = true
            setSupportMultipleWindows(true)
        }

        if (typeMode == "na") {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")
            var nidNL = false
            if (pqr == null || aut == null || ses == null)
                 nidNL = true

            binding.webView.apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        if (url == naQRBase && nidNL) {
                            getCookies(
                                cookie.getCookie(naQRBase)
                            )
                            pm.setString("NID_PQR", "")
                            pm.setString("NID_AUT", "")
                            pm.setString("NID_SES", "")
                        }
                    }
                }
                val delicious = cookie.getCookie(naQRBase)
                if (delicious == null && nidNL)
                    loadUrl("https://nid.naver.com/nidlogin.login?url=${naQRBase}")
                else
                    if (delicious == null) {
                        cookie.setCookie(
                            naQRBase,
                            "NID_PQR=${pm.getString("NID_PQR")};NID_AUT=${pm.getString("NID_AUT")};NID_SES=${pm.getString("NID_SES")};"
                        )
                    }
                    loadUrl(naQRBase)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            val cookie = CookieManager.getInstance()
            cookie.removeAllCookies(null)
            cookie.flush()
        }
        binding.layerDrawer.closeDrawers()
        return false
    }
}