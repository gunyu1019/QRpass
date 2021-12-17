package kr.yhs.qrcheck.activity

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kr.yhs.qrcheck.R
import kotlin.concurrent.timer

class QRImageActivity: MainResourceActivity() {
    override fun onCreate() {
        imageView = view.findViewById(R.id.qrCodeImageView)
        refreshButton = view.findViewById(R.id.refreshBtn)
        timerTextView = view.findViewById(R.id.timerTextView)
    }

    override fun processTimer() {
        var second = 0
        timer(period = 1000, initialDelay = 1000) {
            context.runOnUiThread {
                timerTextView.text = context.getString(R.string.count, 15 - second)
            }
            second++
            if (second == 15) {
                context.runOnUiThread {
                    timerTextView.text = context.getString(R.string.count, 0)
                    refreshButton.visibility = View.VISIBLE
                }
                cancel()
            }
        }
    }
}