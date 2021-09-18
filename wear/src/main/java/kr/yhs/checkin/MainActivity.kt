package kr.yhs.checkin

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import com.google.android.gms.wearable.*
import kr.yhs.checkin.databinding.ActivityMainBinding
import org.jsoup.Jsoup
import kotlin.concurrent.timer

class MainActivity : Activity(), DataClient.OnDataChangedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager
    private lateinit var typeMode: String
    private var privacyNumber: String = ""

    private fun loadImage(base64: String) {
        val base64Image: String = base64.split(",")[1]
        val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        binding.imageView.setImageBitmap(bitmap)
    }

    private fun webMain(map: Map<String, String>? = null) {
        if (typeMode == "na") {
            Thread {
                val pqr: String
                val aut: String
                val ses: String
                if (map == null) {
                    pqr = pm.getString("NID_PQR") ?: ""
                    aut = pm.getString("NID_AUT") ?: ""
                    ses = pm.getString("NID_SES") ?: ""
                } else {
                    pqr = map["NID_PQR"] ?: ""
                    aut = map["NID_AUT"] ?: ""
                    ses = map["NID_SES"] ?: ""
                }
                val response = Jsoup.connect("https://nid.naver.com/login/privacyQR")
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
                            mainProcess(base64)
                            return@runOnUiThread
                        }
                    } else if (wrap.select(".self_box").html() != "") {
                        this@MainActivity.runOnUiThread {
                            warningMessage(getString(R.string.need_authorize))
                            return@runOnUiThread
                        }
                    } else {
                        Log.w("Loding-Failed", html.html())
                    }
                } else if (html.select(".login_wrap").html() != "") {
                    this@MainActivity.runOnUiThread {
                        warningMessage(getString(R.string.need_login))
                        return@runOnUiThread
                    }
                } else {
                    Log.w("Loding-Failed", html.html())
                }
                return@Thread
            }.start()
        }
    }

    private fun mainProcess(image: String) {
        binding.main.visibility = View.VISIBLE
        binding.progressLayout.visibility = View.GONE
        binding.warningLayout.visibility = View.GONE
        binding.refreshBtn.visibility = View.GONE

        loadImage(image)
        binding.privateNumberText.text = privacyNumber
        binding.count.text = getString(R.string.count, 15)
        var second = 0
        timer(period = 1000, initialDelay = 1000) {
            runOnUiThread {
                binding.count.text = getString(R.string.count, 15 - second)
            }
            second++
            if (second == 15) {
                runOnUiThread {
                    binding.refreshBtn.visibility = View.VISIBLE
                }
                cancel()
            }
        }
    }

    private fun warningMessage(comment: String) {
        binding.main.visibility = View.GONE
        binding.progressLayout.visibility = View.GONE
        binding.warningLayout.visibility = View.VISIBLE
        binding.refreshBtn.visibility = View.GONE
        binding.warningMessage.text = comment
    }

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
        binding.main.visibility = View.GONE
        binding.progressLayout.visibility = View.VISIBLE
        binding.warningLayout.visibility = View.GONE

        binding.refreshBtn.setOnClickListener {
            webMain()
        }
        binding.privateNumberText.setOnClickListener {
            if (binding.privateNumberText.text == getString(R.string.description_privacy_key) && privacyNumber != "")
                binding.privateNumberText.text = privacyNumber
            else
                binding.privateNumberText.text = getString(R.string.description_privacy_key)
        }

        pm = PackageManager("checkIn", this@MainActivity)
        val pqr = pm.getString("NID_PQR")
        val aut = pm.getString("NID_AUT")
        val ses = pm.getString("NID_SES")
        typeMode = pm.getString("checkMode").toString()
        Log.d("cookie", "Starting=${typeMode}")
        if (typeMode == "") {
            binding.main.visibility = View.GONE
            binding.progressLayout.visibility = View.GONE
            binding.warningLayout.visibility = View.VISIBLE
            binding.warningMessage.text = getString(R.string.phone)
        } else {
            binding.main.visibility = View.GONE
            binding.progressLayout.visibility = View.VISIBLE
            binding.warningLayout.visibility = View.GONE
        }

        if (typeMode == "na") {
            Log.d("cookie", "NID_PQR=${pqr};NID_AUT=${aut};NID_SES=${ses};")
            webMain(
                mapOf(
                    "NID_PQR" to (pqr ?: ""),
                    "NID_AUT" to (aut ?: ""),
                    "NID_SES" to (ses ?: "")
                )
            )
        }
    }

    override fun onDataChanged(data: DataEventBuffer) {
        data.forEach{ event ->
            Log.i("Wearable-inputData", "data-received ${event.type}")
            if (event.type == DataEvent.TYPE_CHANGED) {
                event.dataItem.also { item ->
                    Log.i("Wearable-inputData", "item-url ${item.uri.path}")
                    if (item.uri.path == "/naver/token") {
                        DataMapItem.fromDataItem(item).dataMap.apply {
                            val pqr = getString("kr.yhs.checkin.token.NID_PQR") ?: ""
                            val aut = getString("kr.yhs.checkin.token.NID_AUT") ?: ""
                            val ses = getString("kr.yhs.checkin.token.NID_SES") ?: ""
                            pm.setString("checkMode", "na")
                            pm.setString("NID_PQR", pqr)
                            pm.setString("NID_AUT", aut)
                            pm.setString("NID_SES", ses)

                            binding.main.visibility = View.GONE
                            binding.progressLayout.visibility = View.VISIBLE
                            binding.warningLayout.visibility = View.GONE
                            webMain(
                                mapOf(
                                    "NID_PQR" to pqr,
                                    "NID_AUT" to aut,
                                    "NID_SES" to ses
                                )
                            )
                        }
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                event.dataItem.also { item ->
                    Log.i("Wearable-inputData", "item-url ${item.uri.path}")
                    if (item.uri.path == "/naver/token") {
                        pm.removeKey("NID_PQR")
                        pm.removeKey("NID_AUT")
                        pm.removeKey("NID_SES")
                    }
                }
            }
        }
    }
}