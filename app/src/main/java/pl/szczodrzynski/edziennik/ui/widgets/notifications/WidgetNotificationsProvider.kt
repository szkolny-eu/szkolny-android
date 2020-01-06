/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-6.
 */

package pl.szczodrzynski.edziennik.ui.widgets.notifications

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.receivers.SzkolnyReceiver
import pl.szczodrzynski.edziennik.ui.widgets.WidgetConfig

class WidgetNotificationsProvider : AppWidgetProvider() {
    companion object {
        private const val TAG = "WidgetNotificationsProvider"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext as App
        val widgetConfigs = app.config.widgetConfigs
        for (appWidgetId in appWidgetIds) {
            val config = widgetConfigs.getJsonObject(appWidgetId.toString())?.let { app.gson.fromJson(it, WidgetConfig::class.java) } ?: continue

            val iconSize = if (config.bigStyle) 24 else 16

            val views: RemoteViews = if (config.bigStyle) {
                RemoteViews(app.packageName, if (config.darkTheme) R.layout.widget_notifications_dark_big else R.layout.widget_notifications_big)
            } else {
                RemoteViews(app.packageName, if (config.darkTheme) R.layout.widget_notifications_dark else R.layout.widget_notifications)
            }

            val syncIntent = Intent(SzkolnyReceiver.ACTION)
            syncIntent.putExtra("task", "SyncRequest")
            val syncPendingIntent = PendingIntent.getBroadcast(context, 0, syncIntent, 0)
            views.setOnClickPendingIntent(R.id.widgetNotificationsSync, syncPendingIntent)

            views.setImageViewBitmap(
                    R.id.widgetNotificationsSync,
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_download_outline)
                            .colorInt(Color.WHITE)
                            .sizeDp(iconSize)
                            .toBitmap()
            )

            views.setViewVisibility(R.id.widgetNotificationsLoading, View.GONE)

            val listIntent = Intent(context, WidgetNotificationsService::class.java)
            listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            listIntent.putExtra("config", app.gson.toJson(config))
            listIntent.data = Uri.parse(listIntent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.widgetNotificationsListView, listIntent)

            val itemIntent = Intent(context, MainActivity::class.java)
            itemIntent.action = Intent.ACTION_MAIN
            val itemPendingIntent = PendingIntent.getActivity(context, 0, itemIntent, 0)
            views.setPendingIntentTemplate(R.id.widgetNotificationsListView, itemPendingIntent)

            val headerIntent = Intent(context, MainActivity::class.java)
            headerIntent.action = Intent.ACTION_MAIN
            headerIntent.putExtra("fragmentId", MainActivity.DRAWER_ITEM_NOTIFICATIONS)
            val headerPendingIntent = PendingIntent.getActivity(context, 0, headerIntent, 0)
            views.setOnClickPendingIntent(R.id.widgetNotificationsHeader, headerPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetNotificationsListView)
        }
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
