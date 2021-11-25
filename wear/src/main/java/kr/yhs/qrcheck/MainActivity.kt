package kr.yhs.qrcheck

import android.app.Activity
import android.os.Bundle
import android.view.View
import kr.yhs.qrcheck.client.BaseClient
import kr.yhs.qrcheck.client.NaverClient
import kr.yhs.qrcheck.databinding.ActivityMainBinding

class MainActivity : Activity() {
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    private lateinit var pm: PackageManager
    private var typeClient: Int = 0

    private lateinit var client: BaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.main.visibility = View.GONE
        binding.progressLayout.visibility = View.VISIBLE
        binding.warningLayout.visibility = View.GONE

        pm = PackageManager("QRpass", this@MainActivity)
        typeClient = pm.getInt("checkMode", default = -1)

        if (typeClient == -1) {
            binding.main.visibility = View.GONE
            binding.progressLayout.visibility = View.GONE
            binding.warningLayout.visibility = View.VISIBLE
            binding.warningMessage.text = getString(R.string.phone)
            return
        } else if(typeClient == 0) {
            val pqr = pm.getString("NID_PQR")
            val aut = pm.getString("NID_AUT")
            val ses = pm.getString("NID_SES")
            client = NaverClient(this)
        }

        binding.main.visibility = View.GONE
        binding.progressLayout.visibility = View.VISIBLE
        binding.warningLayout.visibility = View.GONE
        client.setResource(
            binding.privateCodeText,
            binding.qrCodeImage
        )
    }
}