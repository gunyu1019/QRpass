package kr.yhs.checkin

import android.app.Activity
import android.os.Bundle
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import kr.yhs.checkin.databinding.ActivityMainBinding

class MainActivity : Activity(), DataClient.OnDataChangedListener {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onDataChanged(p0: DataEventBuffer) {
        TODO("Not yet implemented")
    }
}