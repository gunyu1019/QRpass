package kr.yhs.qrcheck

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.wear.phone.interactions.PhoneTypeHelper
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.ConfirmationOverlay
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.await
import kr.yhs.qrcheck.databinding.ActivityWarningBinding
import kotlin.coroutines.CoroutineContext

class WarningActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var remoteActivityHelper: RemoteActivityHelper
    private var mBinding: ActivityWarningBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityWarningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mJob = Job()

        remoteActivityHelper = RemoteActivityHelper(this)

        val intent = getIntent()
        val warnId = intent.getIntExtra("warning_id", R.string.phone)
        val buttonActive = intent.getBooleanExtra("store_btn", false)

        if (buttonActive)
            binding.warningButton.visibility = View.VISIBLE
        else
            binding.warningButton.visibility = View.GONE
        binding.warningMessage.text = getString(warnId)
        binding.warningButton.setOnClickListener {
           openAppStoreProcess()
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
                    .showOn(this@WarningActivity)
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

                ConfirmationOverlay().showOn(this@WarningActivity)
            } catch (cancellationException: CancellationException) {
                // Request was cancelled normally
                throw cancellationException
            } catch (throwable: Throwable) {
                ConfirmationOverlay()
                    .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                    .showOn(this@WarningActivity)
            }
        }
    }

    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    companion object {
        const val CAPABILITY_PHONE_APP = "qrpass_phone_app"
        private const val TAG = "WarningActivity"
        private const val ANDROID_MARKET_APP_URI =
            "market://details?id=kr.yhs.qrpass"
    }
}