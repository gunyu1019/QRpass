package kr.yhs.qrpass.adapter.viewPage

import android.view.View
import kr.yhs.qrpass.MainActivity
import kr.yhs.qrpass.activity.MainResourceActivity

data class ViewData (
    val id: Int,
    var view: View? = null,
    val activity: MainResourceActivity? = null,
    val context: MainActivity
)