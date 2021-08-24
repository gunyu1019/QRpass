package kr.yhs.checkin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.webkit.*
import com.google.android.material.navigation.NavigationView
import kr.yhs.checkin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cookie = CookieManager.getInstance()
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (supportActionBar != null)
            supportActionBar!!.hide()

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            allowFileAccess = true
            setSupportMultipleWindows(true)
        }

        binding.webView.apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    if (url == "https://nid.naver.com/login/privacyQR") {
                        Log.i(
                            "Debug",
                            CookieManager.getInstance()
                                .getCookie("https://nid.naver.com/login/privacyQR")
                        )
                    }
                }
            }

            if (cookie.getCookie("https://nid.naver.com/login/privacyQR") == null)
                loadUrl("https://nid.naver.com/nidlogin.login?url=https://nid.naver.com/login/privacyQR")
            else
                loadUrl("https://nid.naver.com/login/privacyQR")
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