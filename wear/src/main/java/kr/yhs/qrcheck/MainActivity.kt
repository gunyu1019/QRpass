package kr.yhs.qrcheck

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.wear.phone.interactions.PhoneTypeHelper
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.ConfirmationOverlay
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await
import kr.yhs.qrcheck.client.BaseClient
import kr.yhs.qrcheck.client.NaverClient
import kr.yhs.qrcheck.databinding.ActivityMainBinding
import kotlin.concurrent.timer
import kotlin.coroutines.CoroutineContext

class MainActivity : Activity(), CoroutineScope, CapabilityClient.OnCapabilityChangedListener,
    DataClient.OnDataChangedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager
    private var typeClient: Int = 0

    private lateinit var client: BaseClient

    private lateinit var capabilityClient: CapabilityClient
    private lateinit var nodeClient: NodeClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    private var phoneNode: Node? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mJob = Job()

        capabilityClient = Wearable.getCapabilityClient(this)
        nodeClient = Wearable.getNodeClient(this)
        remoteActivityHelper = RemoteActivityHelper(this)

        binding.main.visibility = View.GONE
        binding.progressLayout.visibility = View.VISIBLE
        binding.warningLayout.visibility = View.GONE

        pm = PackageManager("QRpass", this@MainActivity)
        typeClient = pm.getInt("typeClient", default = -1)
        Log.i(TAG, "typeClient: $typeClient")

        if (typeClient == -1) {
            binding.warningButton.setOnClickListener {
                openAppStoreProcess()
            }
            when (PhoneTypeHelper.getPhoneDeviceType(applicationContext)) {
                PhoneTypeHelper.DEVICE_TYPE_ANDROID -> {
                    Log.d(TAG, "\tDEVICE_TYPE_ANDROID")
                }
                PhoneTypeHelper.DEVICE_TYPE_IOS -> {
                    Log.d(TAG, "\tDEVICE_TYPE_IOS")
                    binding.warningMessage.text = getString(R.string.ios_not_support)
                    binding.warningButton.visibility = View.GONE
                    return
                }
                else -> {
                    Log.d(TAG, "\tDEVICE_TYPE_ERROR_UNKNOWN")
                    binding.warningMessage.text = getString(R.string.device_not_support)
                    binding.warningButton.visibility = View.GONE
                    return
                }
            }

            binding.main.visibility = View.GONE
            binding.progressLayout.visibility = View.GONE
            binding.warningLayout.visibility = View.VISIBLE
            binding.warningButton.visibility = View.VISIBLE
            binding.warningMessage.text = getString(R.string.phone)
            return
        } else if(typeClient == 0) {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")
            client = NaverClient(this)
            client.onLoad(pqr, aut, ses)
        }

        binding.main.visibility = View.GONE
        binding.progressLayout.visibility = View.VISIBLE
        binding.warningLayout.visibility = View.GONE

        client.setOnSucceedListener{
            binding.main.visibility = View.VISIBLE
            binding.progressLayout.visibility = View.GONE
            binding.warningLayout.visibility = View.GONE
        }
        client.setOnFailedListener {
            onFailedListener(it)
        }

        client.setResource(
            binding.privateCodeText,
            binding.qrCodeImage
        )
        mainProcess()
    }

    private fun mainProcess() {
        client.setOnSucceedListener {
            binding.refreshBtn.visibility = View.GONE
            binding.timeCount.text = getString(R.string.count, 15)

            var second = 0
            timer(period = 1000, initialDelay = 1000) {
                this@MainActivity.runOnUiThread {
                    binding.timeCount.text = getString(R.string.count, 15 - second)
                }
                second++
                if (second == 15) {
                    this@MainActivity.runOnUiThread {
                        binding.timeCount.text = getString(R.string.count, 0)
                        binding.refreshBtn.visibility = View.VISIBLE
                    }
                    cancel()
                }
            }
        }
        client.getData()
        binding.main.visibility = View.VISIBLE
        binding.progressLayout.visibility = View.GONE
        binding.warningLayout.visibility = View.GONE
    }

    private fun onFailedListener(reason: String) {
        binding.main.visibility = View.GONE
        binding.progressLayout.visibility = View.GONE
        binding.warningLayout.visibility = View.VISIBLE

        when(reason) {
            "loginExpired" -> {
                binding.warningMessage.text = getString(R.string.login_expired)
                binding.warningButton.visibility = View.GONE
            }
            "phoneAuthorize" -> {
                binding.warningMessage.text = getString(R.string.phone_authorize)
                binding.warningButton.visibility = View.GONE
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

    private fun openAppProcess() {
        Log.d(TAG, "openAppProcess()")

        val intent = when (PhoneTypeHelper.getPhoneDeviceType(applicationContext)) {
            PhoneTypeHelper.DEVICE_TYPE_ANDROID -> {
                Log.d(TAG, "\tDEVICE_TYPE_ANDROID")
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

    companion object {
        const val CAPABILITY_PHONE_APP = "qrpass_phone_app"
        private const val TAG = "MainActivity"
        private const val ANDROID_MARKET_APP_URI =
            "market://details?id=kr.yhs.qrpass"
    }

    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

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
                            val pqr = getString("kr.yhs.qrcheck.token.NID_PQR") ?: ""
                            val aut = getString("kr.yhs.qrcheck.token.NID_AUT") ?: ""
                            val ses = getString("kr.yhs.qrcheck.token.NID_SES") ?: ""

                            typeClient = 0
                            pm.setInt("typeClient", typeClient)
                            pm.setString("NID_PQR", pqr)
                            pm.setString("NID_AUT", aut)
                            pm.setString("NID_SES", ses)

                            client = NaverClient(this@MainActivity)
                            client.onLoad(pqr, aut, ses)

                            binding.main.visibility = View.GONE
                            binding.progressLayout.visibility = View.VISIBLE
                            binding.warningLayout.visibility = View.GONE
                        }
                    }
                    client.setOnSucceedListener{
                        binding.main.visibility = View.VISIBLE
                        binding.progressLayout.visibility = View.GONE
                        binding.warningLayout.visibility = View.GONE
                        mainProcess()
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
}