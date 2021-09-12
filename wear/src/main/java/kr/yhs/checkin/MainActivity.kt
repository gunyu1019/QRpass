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

    private var nidNL: Boolean = false

    private fun loadImage(base64: String) {
        val base64Image: String = base64.split(",")[1]
        val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        binding.imageView.setImageBitmap(bitmap)
    }

    private fun webMain() {
        if (typeMode == "na") {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")
            Thread (
                Runnable {
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
                            val number = numberHTML.text()

                            this@MainActivity.runOnUiThread(java.lang.Runnable {
                                binding.main.visibility = View.VISIBLE
                                binding.progressLayout.visibility = View.GONE
                                binding.warningLayout.visibility = View.GONE
                                binding.refreshBtn.visibility = View.GONE

                                loadImage(base64.toString())
                                binding.privateNumberText.text = number
                                binding.count.text = getString(R.string.count, 15)

                                var second = 0
                                timer(period = 1000, initialDelay = 1000) {
                                    runOnUiThread {
                                        binding.count.text = getString(R.string.count, second)
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
                                binding.main.visibility = View.GONE
                                binding.progressLayout.visibility = View.GONE
                                binding.warningLayout.visibility = View.VISIBLE
                                binding.refreshBtn.visibility = View.GONE

                                binding.warningMessage.text = getString(R.string.need_authorize)
                                return@Runnable
                            })
                        }
                    } else if (html.select(".login_wrap").html() != "") {
                        this@MainActivity.runOnUiThread(java.lang.Runnable {
                            binding.main.visibility = View.GONE
                            binding.progressLayout.visibility = View.GONE
                            binding.warningLayout.visibility = View.VISIBLE
                            binding.refreshBtn.visibility = View.GONE

                            binding.warningMessage.text = getString(R.string.need_login)
                            return@Runnable
                        })
                    }
                }
            ).start()
        }
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
            if ((pqr == null || aut == null || ses == null) || (pqr == "" || aut == "" || ses == ""))
                nidNL = true
            webMain()
        }
    }

    override fun onDataChanged(data: DataEventBuffer) {
        data.forEach{ event ->
            Log.d("Wearable-inputData", "data-received ${event.type}")
            if (event.type == DataEvent.TYPE_CHANGED) {
                event.dataItem.also { item ->
                    Log.d("Wearable-inputData", "item-url ${item.uri.path}")
                    if (item.uri.path == "/naKey") {
                        DataMapItem.fromDataItem(item).dataMap.apply {
                            val pqr = getString("kr.yhs.checkin.na.NID_PQR")
                            val aut = getString("kr.yhs.checkin.na.NID_AUT")
                            val ses = getString("kr.yhs.checkin.na.NID_SES")
                            var nidNL = false
                            if ((pqr == null || aut == null || ses == null) || (pqr == "" || aut == "" || ses == ""))
                                nidNL = true

                            pm.setString("checkMode", "na")
                            if (!nidNL) {
                                pm.setString("NID_PQR", pqr)
                                pm.setString("NID_AUT", aut)
                                pm.setString("NID_SES", ses)

                                binding.main.visibility = View.GONE
                                binding.progressLayout.visibility = View.VISIBLE
                                binding.warningLayout.visibility = View.GONE
                                webMain()
                            }
                        }
                    }
                }
            }
        }
    }
}