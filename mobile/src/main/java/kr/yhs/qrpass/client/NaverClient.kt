package kr.yhs.qrpass.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class NaverClient: BaseClient() {
    private lateinit var keyPQR: String
    private lateinit var keyAUT: String
    private lateinit var keySES: String

    override val baseLink = "https://nid.naver.com/login/privacyQR"

    override fun checkBaseLink(url: String): Boolean {
        return when {
            url.indexOf("https://nid.naver.com/nidlogin.login") == 0 -> true
            url.indexOf("https://nid.naver.com/iasystem/mobile_pop.nhn") == 0 -> true
            else -> false
        }
    }

    override fun <T> onLoad(vararg args: T) {
        keyPQR = args[0].toString()
        keyAUT = args[1].toString()
        keySES = args[2].toString()
        super.onLoad(args)
    }

    override fun getData() {
        launch {
            val deferred = async(Dispatchers.Default) {
                var result = mapOf<String, Any>(
                    "status" to false
                )
                val response = Jsoup.connect(baseLink)
                    .header("Cookie", "NID_PQR=${keyPQR};NID_AUT=${keyAUT};NID_SES=${keySES};")
                    .get()
                val html = response.body()
                if (html.select("div.qr_wrap").html() != "") {
                    val wrap = html.select("div.qr_wrap")
                    if (wrap.select("div.qr_area").html() != "") {
                        val area = wrap.select("div.qr_area")
                        val base64 = area.select("div.qr_box img").attr("src")
                        val numberHTML = area.select("div.number_box span.number")
                        val privateKey = numberHTML.text()

                        result = mapOf<String, Any>(
                            "status" to true,
                            "privateKey" to privateKey,
                            "qrCodeKey" to base64
                        )
                    } else if (wrap.select(".self_box").html() != "") {
                        result = mapOf<String, Any>(
                            "status" to false,
                            "reason" to "phoneAuthorize"
                        )
                    }
                } else if (html.select(".login_wrap").html() != "") {
                    result = mapOf<String, Any>(
                        "status" to false,
                        "reason" to "loginExpired"
                    )
                }
                return@async result
            }
            deferred.await().let {
                if(it["status"] as Boolean) {
                    responseStatus = true
                    val privateKeyResponse = (it["privateKey"] as String?)!!
                    val qrImageResponse = (it["qrCodeKey"] as String?)!!
                    onSucceed(privateKeyResponse, qrImageResponse)
                } else {
                    responseStatus = false
                    val responseReason = (it["reason"] as String?)?:""
                    onFailed(responseReason)
                }
            }
        }
    }
}