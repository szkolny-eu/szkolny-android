/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-6.
 */

package pl.szczodrzynski.edziennik.ui.widgets.notifications

import android.content.Intent
import android.database.Cursor
import android.os.Binder
import android.widget.AdapterView
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.JsonParser
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.data.enums.NotificationType
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.widgets.WidgetConfig
import pl.szczodrzynski.edziennik.utils.models.Date

class WidgetNotificationsFactory(val app: App, val config: WidgetConfig) : RemoteViewsService.RemoteViewsFactory {
    companion object {
        private const val TAG = "WidgetNotificationsFactory"
    }
    private var cursor: Cursor? = null

    override fun onDataSetChanged() {
        cursor?.close()
        Binder.clearCallingIdentity().let {
            cursor = app.db.notificationDao().getAllCursor()
            Binder.restoreCallingIdentity(it)
        }
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position == AdapterView.INVALID_POSITION || cursor?.moveToPosition(position) != true)
            return null

        val views: RemoteViews = if (config.bigStyle) {
            RemoteViews(app.packageName, if (config.darkTheme) R.layout.row_widget_notifications_dark_big_item else R.layout.row_widget_notifications_big_item)
        } else {
            RemoteViews(app.packageName, if (config.darkTheme) R.layout.row_widget_notifications_dark_item else R.layout.row_widget_notifications_item)
        }

        val notification = cursor?.run {
            Notification(
                    getLong("id") ?: 0,
                    getString("title") ?: "",
                    getString("text") ?: "",
                    getString("textLong"),
                    getInt("type")?.asNotificationTypeOrNull() ?: NotificationType.GENERAL,
                    getInt("profileId"),
                    getString("profileName"),
                    getInt("posted") == 1,
                    getInt("viewId")?.asNavTargetOrNull(),
                    getString("extras")?.let { JsonParser.parseString(it).asJsonObject },
                    getLong("addedDate") ?: System.currentTimeMillis()
            )
        } ?: return views

        views.apply {
            setTextViewText(R.id.widgetNotificationsTitle,
                    app.getString(R.string.widget_notifications_title_format, notification.title, notification.type.titleRes.resolveString(app)))
            setTextViewText(R.id.widgetNotificationsText, notification.text)
            setTextViewText(R.id.widgetNotificationsDate, Date.fromMillis(notification.addedDate).formattedString)
            setOnClickFillInIntent(R.id.widgetNotificationsRoot, Intent().also { notification.fillIntent(it) })
        }

        return views
    }

    override fun getItemId(position: Int): Long = if (cursor?.moveToPosition(position) == true)
        cursor?.getLong("id") ?: position.toLong() else position.toLong()
    override fun getCount() = cursor?.count ?: 0
    override fun onCreate() {}
    override fun getLoadingView() = null
    override fun hasStableIds() = true
    override fun getViewTypeCount() = 1
    override fun onDestroy() = cursor?.close() ?: Unit
}
