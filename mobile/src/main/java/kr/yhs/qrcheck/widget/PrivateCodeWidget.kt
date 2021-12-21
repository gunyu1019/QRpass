package kr.yhs.qrcheck.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kr.yhs.qrcheck.PackageManager
import kr.yhs.qrcheck.client.BaseClient

class PrivateCodeWidget: AppWidgetProvider() {
    lateinit var client: BaseClient
    private lateinit var pm: PackageManager
    private var typeClient: Int = 0

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        if (context != null) {
            pm = PackageManager("QRpass", context)
            typeClient = pm.getInt("clientMode", -1)
            if (typeClient == -1) return

        }
    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}