package kr.yhs.qrcheck.activity

import android.widget.ImageView
import kr.yhs.qrcheck.R

class PrivateCodeActivity: MainResourceActivity() {
    override fun onCreate() {
        privateCodeTextView = view.findViewById(R.id.privateCode)
        if (context.clientInitialized) {
            context.client.privateKeyResource = privateCodeTextView
            if (context.client.privateKeyResponse != null) {
                privateCodeTextView.text = context.client.privateKeyResponse
            } else {
                context.client.getData()
            }
        }

        val button = view.findViewById<ImageView>(R.id.nextButton)
        button.setOnClickListener {
            context.nextPage()
        }


    }
}