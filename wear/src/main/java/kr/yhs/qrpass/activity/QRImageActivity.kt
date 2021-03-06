package kr.yhs.qrpass.activity

import android.view.View
import android.widget.ProgressBar
import kr.yhs.qrpass.R
import kotlin.concurrent.timer

class QRImageActivity: MainResourceActivity() {
    private lateinit var processBar: ProgressBar

    override fun onCreate() {
        imageView = view.findViewById(R.id.qrCodeImageView)
        refreshButton = view.findViewById(R.id.refreshBtn)
        timerTextView = view.findViewById(R.id.timerTextView)
        processBar = view.findViewById(R.id.circularProgressIndicator)

        timerTextView.text = context.getString(R.string.count, 15)
        refreshButton.visibility = View.GONE

        refreshButton.setOnClickListener {
            timerProcess()
        }
        if (context.clientInitialized) {
            context.client.qrImageResource = imageView
            timerProcess()
        }
    }

    private fun timerProcess() {
        var second = 0
        context.client.getData()
        refreshButton.visibility = View.GONE
        timerTextView.text = context.getString(R.string.count, 15)
        processBar.progress = 150
        timer(period = 1000, initialDelay = 1000) {
            context.runOnUiThread {
                timerTextView.text = context.getString(R.string.count, 15 - second)
                processBar.progress = (15 - second) * 10
            }
            second++
            if (second == 15) {
                context.runOnUiThread {
                    timerTextView.text = context.getString(R.string.count, 0)
                    refreshButton.visibility = View.VISIBLE
                    processBar.progress = 0
                }
                cancel()
            }
        }
    }
}