package kr.yhs.qrcheck.adapter.viewPage

import android.app.Activity
import android.view.View
import kr.yhs.qrcheck.activity.MainResourceActivity

data class ViewData (
    val id: Int,
    var view: View? = null,
    val activity: MainResourceActivity? = null,
    val context: Activity
)