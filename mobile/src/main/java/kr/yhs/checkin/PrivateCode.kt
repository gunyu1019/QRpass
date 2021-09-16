package kr.yhs.checkin

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.widget.RemoteViews


class PrivateCode : AppWidgetProvider() {
    private val getPrivateKeySimple = "getPrivateKeySimple"

    private fun getPendingSelfIntent(context: Context?, action: String): PendingIntent? {
        val intent = Intent(context, PrivateCode::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(context, 0, intent, 0)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            val widgetText = context.getString(R.string.private_code_description)
            val views = RemoteViews(context.packageName, R.layout.private_code)
            views.setTextViewText(R.id.privateCodeWidgetSimple, widgetText)
            views.setOnClickPendingIntent(R.id.privateCodeWidgetSimple, getPendingSelfIntent(context, getPrivateKeySimple))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (getPrivateKeySimple == intent?.action) {

        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Enter relevant functionality for when the last widget is disabled
    }
}