/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-7.
 */

package pl.szczodrzynski.edziennik.ui.widgets.luckynumber

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.getJsonObject
import pl.szczodrzynski.edziennik.ext.pendingIntentFlag
import pl.szczodrzynski.edziennik.ui.widgets.WidgetConfig
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class WidgetLuckyNumberProvider : AppWidgetProvider() {
    companion object {
        private const val TAG = "WidgetLuckyNumberProvider"
    }

    private fun getRemoteViews(app: App, config: WidgetConfig): RemoteViews {
        return if (config.bigStyle) {
            RemoteViews(app.packageName, if (config.darkTheme) R.layout.widget_lucky_number_dark_big else R.layout.widget_lucky_number_big)
        } else {
            RemoteViews(app.packageName, if (config.darkTheme) R.layout.widget_lucky_number_dark else R.layout.widget_lucky_number)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext as App
        val widgetConfigs = app.config.widgetConfigs
        for (appWidgetId in appWidgetIds) {
            val config = widgetConfigs.getJsonObject(appWidgetId.toString())?.let { app.gson.fromJson(it, WidgetConfig::class.java) } ?: continue

            val views = getRemoteViews(app, config)

            val today = Date.getToday()
            val tomorrow = Date.getToday().stepForward(0, 0, 1)

            val profile = app.db.profileDao().getByIdNow(config.profileId)
            val luckyNumber = app.db.luckyNumberDao().getNearestFutureNow(config.profileId, today)
            val isYours = luckyNumber?.number == profile?.studentNumber

            var noNumberText = false

            if (profile != null) {
                views.setTextViewText(R.id.widgetLuckyNumberProfileRight, profile.name)
                views.setTextViewText(R.id.widgetLuckyNumberProfileBottom, profile.name)
            }

            if (profile == null || luckyNumber == null || luckyNumber.number == -1) {
                noNumberText = true
                views.setTextViewText(R.id.widgetLuckyNumberTextRight, null)
                views.setTextViewText(R.id.widgetLuckyNumberTextBottom, null)
            }
            else {
                views.setTextViewText(R.id.widgetLuckyNumberTextRight, luckyNumber.number.toString())
                views.setTextViewText(R.id.widgetLuckyNumberTextBottom, luckyNumber.number.toString())
            }

            val drawableRes = when {
                luckyNumber == null || luckyNumber.number == -1 -> R.drawable.emoji_sad
                isYours -> R.drawable.emoji_glasses
                !isYours -> R.drawable.emoji_smiling
                else -> R.drawable.emoji_no_face
            }

            views.setViewVisibility(R.id.widgetLuckyNumberTextRightLayout, if (noNumberText) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widgetLuckyNumberTextBottomLayout, if (noNumberText) View.GONE else View.VISIBLE)
            views.setImageViewResource(R.id.widgetLuckyNumberIcon, drawableRes)

            updateLayout(config, views, appWidgetManager, appWidgetId)

            val openIntent = Intent(context, MainActivity::class.java)
            openIntent.action = Intent.ACTION_MAIN
            openIntent.putExtra("fragmentId", MainActivity.DRAWER_ITEM_HOME)
            val openPendingIntent = PendingIntent.getActivity(context, 0, openIntent, pendingIntentFlag())
            views.setOnClickPendingIntent(R.id.widgetLuckyNumberRoot, openPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun updateLayout(config: WidgetConfig, views: RemoteViews, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val width = Utils.getCellsForSize(minWidth)
        val height = Utils.getCellsForSize(minHeight)
        when (width) {
            1 -> {
                views.setViewVisibility(R.id.widgetLuckyNumberProfileRight, View.GONE)
                views.setViewVisibility(R.id.widgetLuckyNumberProfileBottom, View.GONE)
                views.setViewVisibility(R.id.widgetLuckyNumberTextRight, View.GONE)
                views.setViewVisibility(R.id.widgetLuckyNumberTextBottom, View.VISIBLE)
            }
            2 -> {
                views.setViewVisibility(R.id.widgetLuckyNumberProfileRight, View.GONE)
                views.setViewVisibility(R.id.widgetLuckyNumberProfileBottom, View.VISIBLE)
                views.setViewVisibility(R.id.widgetLuckyNumberTextRight, View.VISIBLE)
                views.setViewVisibility(R.id.widgetLuckyNumberTextBottom, View.GONE)
            }
            else -> {
                views.setViewVisibility(R.id.widgetLuckyNumberProfileRight, if (config.bigStyle) View.GONE else View.VISIBLE)
                views.setViewVisibility(R.id.widgetLuckyNumberProfileBottom, if (config.bigStyle) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widgetLuckyNumberTextRight, View.VISIBLE)
                views.setViewVisibility(R.id.widgetLuckyNumberTextBottom, View.GONE)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        val app = context.applicationContext as App
        val widgetConfigs = app.config.widgetConfigs
        val config = widgetConfigs.getJsonObject(appWidgetId.toString())?.let { app.gson.fromJson(it, WidgetConfig::class.java) } ?: return
        val views: RemoteViews = getRemoteViews(app, config)
        updateLayout(config, views, appWidgetManager, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val app = context.applicationContext as App
        val widgetConfigs = app.config.widgetConfigs
        appWidgetIds.forEach {
            widgetConfigs.remove(it.toString())
        }
        app.config.widgetConfigs = widgetConfigs
    }
}
