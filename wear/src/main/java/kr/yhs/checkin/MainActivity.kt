package kr.yhs.checkin

import android.app.Activity
import android.os.Bundle
import kr.yhs.checkin.databinding.ActivityMainBinding

class MainActivity : Activity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}