package kr.yhs.qrcheck.activity

import android.widget.TextView
import kr.yhs.qrcheck.R

class PrivateCodeActivity: MainResourceActivity() {
    override fun onCreate() {
        privateCodeTextView = view.findViewById(R.id.privateCode)
    }
}