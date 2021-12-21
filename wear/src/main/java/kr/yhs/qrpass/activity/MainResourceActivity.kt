package kr.yhs.qrpass.activity

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kr.yhs.qrpass.MainActivity
import kr.yhs.qrpass.adapter.viewPage.ViewData

abstract class MainResourceActivity {
    var id = 0
    lateinit var view: View
    lateinit var context: MainActivity

    // Customize (Resource)
    open lateinit var privateCodeTextView: TextView
    open lateinit var imageView: ImageView
    open lateinit var refreshButton: ImageView
    open lateinit var timerTextView: TextView

    fun loadData(data: ViewData) {
        if (data.activity != this)
            return
        this.id = data.id
        this.view = data.view!!
        this.context = data.context
    }

    abstract fun onCreate()

    // Customize (Function)
    open fun processTimer() {
        return
    }
}