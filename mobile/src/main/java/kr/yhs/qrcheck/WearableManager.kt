package kr.yhs.checkin

import android.app.Activity
import android.os.Looper
import android.util.Log
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kr.yhs.qrcheck.MainActivity
import kotlin.coroutines.CoroutineContext


class WearableManager : CapabilityClient.OnCapabilityChangedListener, CoroutineScope {
    private lateinit var pm: PackageManager
    private lateinit var dataClient: DataClient

    private lateinit var capabilityClient: CapabilityClient
    private lateinit var nodeClient: NodeClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    private var wearNodesWithApp: Set<Node>? = null
    private var allConnectedNodes: List<Node>? = null

    val NAVER_TOKEN = "/naver/token"

    fun loadClient(context: MainActivity) {
        mJob = Job()

        pm = PackageManager("QRpass", context)
        dataClient = Wearable.WearableOptions.Builder().setLooper(
            Looper.getMainLooper()
        ).build().let { options ->
            Wearable.getDataClient(context, options)
        }

        capabilityClient = Wearable.getCapabilityClient(context)
        nodeClient = Wearable.getNodeClient(context)
        remoteActivityHelper = RemoteActivityHelper(context)
    }

    fun insertData(
        key: String,
        data: Map<String, Any>,
        successListener: OnSuccessListener<Any>? = null,
        failureListener: OnFailureListener? = null
    ) {
        Log.i("$TAG [key]", key)
        Log.i("$TAG [value]", data.toString())
        val putDataReq: PutDataRequest = PutDataMapRequest.create(key).run {
            data.forEach { (key, value) ->
                Log.d("$TAG [data-forEach]", "$key: $value")
                when (value) {
                    is String -> dataMap.putString(key, value)
                    is Int -> dataMap.putInt(key, value)
                    is Boolean -> dataMap.putBoolean(key, value)
                    is Byte -> dataMap.putByte(key, value)
                    is Double -> dataMap.putDouble(key, value)
                    is Float -> dataMap.putFloat(key, value)
                }
            }
            asPutDataRequest().setUrgent()
        }
        dataClient.putDataItem(putDataReq).apply {
            if (successListener != null) {
                addOnSuccessListener(successListener)
            }
            if (failureListener != null) {
                addOnFailureListener(failureListener)
            }
        }
    }

    fun onPause() {
        Log.d(TAG, "onPause()")
        capabilityClient.removeListener(this, CAPABILITY_WEAR_APP)
    }

    fun onResume() {
        Log.d(TAG, "onResume()")
        capabilityClient.addListener(this, CAPABILITY_WEAR_APP)
    }

    private suspend fun findWearDevices() {
        Log.d(TAG, "findWearDevices()")

        try {
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_WEAR_APP, CapabilityClient.FILTER_ALL)
                .await()

            withContext(Dispatchers.Main) {
                Log.d(TAG, "Capability request succeeded.")
                wearNodesWithApp = capabilityInfo.nodes
                Log.d(TAG, "Capable Nodes: $wearNodesWithApp")
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
            throw cancellationException
        } catch (throwable: Throwable) {
            Log.d(TAG, "Capability request failed to return any results.")
        }
    }

    private suspend fun findAllConnections() {
        Log.d(TAG, "findAllConnections()")

        try {
            val connectedNodes = nodeClient.connectedNodes.await()

            withContext(Dispatchers.Main) {
                allConnectedNodes = connectedNodes
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
        } catch (throwable: Throwable) {
            Log.d(TAG, "Node request failed to return any results.")
        }
    }

    companion object {
        private const val TAG = "WearableManager"
        private const val CAPABILITY_WEAR_APP = "qrpass_wear_app"
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged(): $capabilityInfo")
        wearNodesWithApp = capabilityInfo.nodes

        launch {
            findAllConnections()
        }
    }

    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main
}