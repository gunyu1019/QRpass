package kr.yhs.checkin

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import kr.yhs.checkin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private val pm = PackageManager("checkIn", this@MainActivity)
    private val naQRBase = "https://nid.naver.com/login/privacyQR"

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
        binding.navigationView.setNavigationItemSelectedListener(this)

        if (typeMode == "na") {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")

            Log.d("cookie", "NID_PQR=${pqr};NID_AUT=${aut};NID_SES=${ses};")
            Log.d("cookie", "cookie=${cookie.getCookie(naQRBase)}")

            var nidNL = false
            if ((pqr == null || aut == null || ses == null) || (pqr == "" || aut == "" || ses == ""))
                nidNL = true
            Log.d("NaverID", "$nidNL")

            binding.webView.apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        if (url == naQRBase && nidNL) {
                            val data = getCookies(
                                cookie.getCookie(naQRBase)
                            )

                            Log.d("cookie", "$data")

                            pm.setString("NID_PQR", data["NID_PQR"] ?: "")
                            pm.setString("NID_AUT", data["NID_AUT"] ?: "")
                            pm.setString("NID_SES", data["NID_SES"] ?: "")
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
                            "NID_PQR=${pqr};NID_AUT=${aut};NID_SES=${ses};"
                        )
                    }
                loadUrl(naQRBase)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            Log.d("onNavigationItemSelected", "${item.itemId}")
            val cookie = CookieManager.getInstance()
            cookie.removeAllCookies(null)
            cookie.flush()

            pm.removeKey("NID_PQR")
            pm.removeKey("NID_AUT")
            pm.removeKey("NID_SES")
            binding.webView.loadUrl("https://nid.naver.com/nidlogin.login?url=${naQRBase}")
        }
        binding.layerDrawer.closeDrawers()

        return false
    }

    override fun onBackPressed() {
        if (binding.layerDrawer.isDrawerOpen(GravityCompat.END)) {
            binding.layerDrawer.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }
}