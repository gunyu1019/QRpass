package kr.yhs.qrpass

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.InputDeviceCompat
import androidx.core.view.ViewConfigurationCompat
import androidx.wear.phone.interactions.PhoneTypeHelper
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.tiles.TileService
import androidx.wear.widget.ConfirmationOverlay
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await
import kr.yhs.qrpass.activity.PrivateCodeActivity
import kr.yhs.qrpass.activity.QRImageActivity
import kr.yhs.qrpass.adapter.viewPage.ViewData
import kr.yhs.qrpass.adapter.viewPage.ViewPagerAdapter
import kr.yhs.qrpass.client.BaseClient
import kr.yhs.qrpass.client.NaverClient
import kr.yhs.qrpass.databinding.ActivityMainBinding
import kotlin.coroutines.CoroutineContext

class MainActivity : Activity(), CoroutineScope , CapabilityClient.OnCapabilityChangedListener,
    DataClient.OnDataChangedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager
    private var typeClient: Int = 0
    private var page: ArrayList<ViewData> = ArrayList()

    lateinit var client: BaseClient
    var clientInitialized: Boolean = false

    private lateinit var capabilityClient: CapabilityClient
    private lateinit var nodeClient: NodeClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    private lateinit var qrActivity: ViewData
    private lateinit var privateCodeActivity: ViewData

    private var phoneNode: Node? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mJob = Job()

        capabilityClient = Wearable.getCapabilityClient(this)
        nodeClient = Wearable.getNodeClient(this)
        remoteActivityHelper = RemoteActivityHelper(this)

        pm = PackageManager("QRpass", this@MainActivity)
        typeClient = pm.getInt("typeClient", default = -1)
        Log.i(TAG, "typeClient: $typeClient")

        privateCodeActivity =
            ViewData(
                R.layout.private_code,
                activity = PrivateCodeActivity(),
                context = this@MainActivity
            )
        qrActivity =
            ViewData(
                R.layout.qr_image,
                activity = QRImageActivity(),
                context = this@MainActivity
            )
        page.add(privateCodeActivity)
        page.add(qrActivity)

        binding.warningLayout.visibility = View.GONE
        binding.viewPager.adapter = ViewPagerAdapter(page)

        val tileClickableId  = intent.getStringExtra(TileService.EXTRA_CLICKABLE_ID)
        if (tileClickableId != null) {
            if (tileClickableId == "Trigger-QRpass-imageTask") {
                binding.viewPager.currentItem = 1
            }
        }

        if (typeClient == -1) {
            when (PhoneTypeHelper.getPhoneDeviceType(applicationContext)) {
                PhoneTypeHelper.DEVICE_TYPE_ANDROID -> {
                    Log.d(TAG, "\tDEVICE_TYPE_ANDROID")
                }
                PhoneTypeHelper.DEVICE_TYPE_IOS -> {
                    Log.d(TAG, "\tDEVICE_TYPE_IOS")
                    warningProcess(
                        R.string.ios_not_support, false
                    )
                    return
                }
                else -> {
                    Log.d(TAG, "\tDEVICE_TYPE_ERROR_UNKNOWN")
                    warningProcess(
                        R.string.device_not_support, false
                    )
                    return
                }
            }

            warningProcess(
                R.string.phone, true
            )
            return
        } else if(typeClient == 0) {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")
            client = NaverClient()
            clientInitialized = true
            client.onLoad(pqr, aut, ses)
        }

        initProcess()
    }

    private fun openAppStoreProcess() {
        Log.d(TAG, "openAppStoreProcess()")

        val intent = when (PhoneTypeHelper.getPhoneDeviceType(applicationContext)) {
            PhoneTypeHelper.DEVICE_TYPE_ANDROID -> {
                Log.d(TAG, "\tDEVICE_TYPE_ANDROID")
                // Create Remote Intent to open Play Store listing of app on remote device.
                Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse(ANDROID_MARKET_APP_URI))
            }
            PhoneTypeHelper.DEVICE_TYPE_IOS -> {
                Log.d(TAG, "\tDEVICE_TYPE_IOS")

                ConfirmationOverlay()
                    .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                    .showOn(this@MainActivity)
                return
            }
            else -> {
                Log.d(TAG, "\tDEVICE_TYPE_ERROR_UNKNOWN")
                return
            }
        }

        launch {
            try {
                remoteActivityHelper.startRemoteActivity(intent).await()

                ConfirmationOverlay().showOn(this@MainActivity)
            } catch (cancellationException: CancellationException) {
                // Request was cancelled normally
                throw cancellationException
            } catch (throwable: Throwable) {
                ConfirmationOverlay()
                    .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                    .showOn(this@MainActivity)
            }
        }
    }

    private fun initProcess() {
        client.setOnSucceedListener{
            binding.progressLayout.visibility = View.GONE
            binding.warningLayout.visibility = View.GONE
        }

        client.setOnFailedListener {
            onFailedListener(it)
        }
    }

    private fun onFailedListener(reason: String) {
        when(reason) {
            "loginExpired" -> {
                warningProcess(
                    R.string.login_expired, false
                )
            }
            "phoneAuthorize" -> {
                warningProcess(
                    R.string.phone_authorize, false
                )
            }
        }
    }


    override fun onPause() {
        super.onPause()
        Wearable.getCapabilityClient(this).removeListener(this, CAPABILITY_PHONE_APP)
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onResume() {
        super.onResume()
        Wearable.getCapabilityClient(this).addListener(this, CAPABILITY_PHONE_APP)
        Wearable.getDataClient(this).addListener(this)
        launch {
            connectionProcess()
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)) {
            val delta = event.getAxisValue(MotionEvent.AXIS_SCROLL) * ViewConfigurationCompat.getScaledVerticalScrollFactor(
                ViewConfiguration.get(this), this
            )
            if (delta > 0 && binding.viewPager.currentItem <= binding.viewPager.adapter!!.itemCount)
                binding.viewPager.currentItem += 1
            else if (delta < 0 && binding.viewPager.currentItem >= 0)
                binding.viewPager.currentItem -= 1
        }
        return super.onGenericMotionEvent(event)
    }

    fun nextPage() {
        if (binding.viewPager.currentItem <= binding.viewPager.adapter!!.itemCount)
            binding.viewPager.currentItem += 1
        return
    }

    private fun connectionProcess() {
        launch {
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_ALL)
                .await()
            Log.d(TAG, "Capability request succeeded.")

            withContext(Dispatchers.Main) {
                phoneNode = capabilityInfo.nodes.firstOrNull()
            }
        }
    }

    companion object {
        const val CAPABILITY_PHONE_APP = "qrpass_phone_app"
        private const val TAG = "MainActivity"
        private const val ANDROID_MARKET_APP_URI =
            "market://details?id=kr.yhs.qrpass"
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged(): $capabilityInfo")
        phoneNode = capabilityInfo.nodes.firstOrNull()
    }

    override fun onDataChanged(data: DataEventBuffer) {
        data.forEach{ event ->
            Log.i(TAG, "onDataChanged(): data-received ${event.type}")
            if (event.type == DataEvent.TYPE_CHANGED) {
                event.dataItem.also { item ->
                    Log.i(TAG, "onDataChanged(): item-url ${item.uri.path}")
                    if (item.uri.path == "/naver/token") {
                        DataMapItem.fromDataItem(item).dataMap.apply {
                            val pqr = getString("kr.yhs.qrpass.token.NID_PQR") ?: ""
                            val aut = getString("kr.yhs.qrpass.token.NID_AUT") ?: ""
                            val ses = getString("kr.yhs.qrpass.token.NID_SES") ?: ""

                            typeClient = 0
                            pm.setInt("typeClient", typeClient)
                            pm.setString("NID_PQR", pqr)
                            pm.setString("NID_AUT", aut)
                            pm.setString("NID_SES", ses)

                            client = NaverClient()
                            clientInitialized = true
                            client.onLoad(pqr, aut, ses)

                            binding.progressLayout.visibility = View.VISIBLE
                            binding.warningLayout.visibility = View.GONE
                            initProcess()
                        }
                    }
                    client.setOnFailedListener {
                        onFailedListener(it)
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                event.dataItem.also { item ->
                    Log.i(TAG, "onDataChanged(): item-url ${item.uri.path}")
                    if (item.uri.path == "/naver/token") {
                        pm.removeKey("NID_PQR")
                        pm.removeKey("NID_AUT")
                        pm.removeKey("NID_SES")
                    }
                }
            }
        }
    }

    private fun warningProcess(
        warnId: Int, buttonActive: Boolean
    ) {
        binding.warningLayout.visibility = View.VISIBLE
        if (buttonActive)
            binding.warningButton.visibility = View.VISIBLE
        else
            binding.warningButton.visibility = View.GONE
        binding.warningMessage.text = getString(warnId)
        binding.warningButton.setOnClickListener {
            openAppStoreProcess()
        }
    }

    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main
}