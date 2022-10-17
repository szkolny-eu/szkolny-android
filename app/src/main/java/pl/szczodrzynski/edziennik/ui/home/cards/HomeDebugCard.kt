/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-22.
 */

package pl.szczodrzynski.edziennik.ui.home.cards

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.plusAssign
import androidx.core.view.setMargins
import androidx.work.WorkManager
import com.chuckerteam.chucker.api.Chucker
import com.hypertrack.hyperlog.HyperLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.CardHomeDebugBinding
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.captcha.RecaptchaPromptDialog
import pl.szczodrzynski.edziennik.ui.home.HomeCard
import pl.szczodrzynski.edziennik.ui.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.home.HomeFragment
import pl.szczodrzynski.edziennik.ui.widgets.WidgetConfigActivity
import pl.szczodrzynski.edziennik.ui.widgets.luckynumber.WidgetLuckyNumberProvider
import pl.szczodrzynski.edziennik.ui.widgets.notifications.WidgetNotificationsProvider
import pl.szczodrzynski.edziennik.ui.widgets.timetable.WidgetTimetableProvider
import kotlin.coroutines.CoroutineContext

class HomeDebugCard(
        override val id: Int,
        val app: App,
        val activity: MainActivity,
        val fragment: HomeFragment,
        val profile: Profile
) : HomeCard, CoroutineScope {
    companion object {
        private const val TAG = "HomeDebugCard"
    }

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun bind(position: Int, holder: HomeCardAdapter.ViewHolder) { launch {
        holder.root.removeAllViews()
        val b = CardHomeDebugBinding.inflate(LayoutInflater.from(holder.root.context))
        b.root.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(8.dp)
        }
        holder.root += b.root

        b.migrate71.onClick {
            app.db.compileStatement("DELETE FROM messages WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);").executeUpdateDelete()
            app.db.compileStatement("DELETE FROM messageRecipients WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);").executeUpdateDelete()
            app.db.compileStatement("DELETE FROM teachers WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);").executeUpdateDelete()
            app.db.compileStatement("DELETE FROM endpointTimers WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0);").executeUpdateDelete()
            app.db.compileStatement("DELETE FROM metadata WHERE profileId IN (SELECT profileId FROM profiles WHERE archived = 0) AND thingType = 8;").executeUpdateDelete()
            app.db.compileStatement("UPDATE profiles SET empty = 1 WHERE archived = 0;").executeUpdateDelete()
            app.db.compileStatement("UPDATE profiles SET lastReceiversSync = 0 WHERE archived = 0;").executeUpdateDelete()
            app.profile.lastReceiversSync = 0
            app.profile.empty = true
        }

        b.syncReceivers.onClick {
            EdziennikTask.recipientListGet(profile.id).enqueue(activity)
        }


        b.pruneWorkButton.onClick {
            WorkManager.getInstance(app).pruneWork()
        }

        b.runChucker.onClick {
            app.startActivity(Chucker.getLaunchIntent(activity, 1));
        }

        b.getLogs.onClick {
            val logs = HyperLog.getDeviceLogsInFile(activity, true)
            val intent = Intent(Intent.ACTION_SEND)

            if (logs.exists()) {
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + logs.absolutePath))
                intent.putExtra(Intent.EXTRA_SUBJECT, "Share debug logs")
                intent.putExtra(Intent.EXTRA_TEXT, "Share debug logs")
                app.startActivity(Intent.createChooser(intent, "Share debug logs"))
            }
        }

        b.refreshWidget.onClick {
            for (widgetType in 0..2) {
                val theClass = when (widgetType) {
                    WidgetConfigActivity.WIDGET_TIMETABLE -> WidgetTimetableProvider::class.java
                    WidgetConfigActivity.WIDGET_NOTIFICATIONS -> WidgetNotificationsProvider::class.java
                    WidgetConfigActivity.WIDGET_LUCKY_NUMBER -> WidgetLuckyNumberProvider::class.java
                    else -> WidgetTimetableProvider::class.java
                }
                val intent = Intent(app, theClass)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(app).getAppWidgetIds(ComponentName(app, theClass)))
                app.sendBroadcast(intent)
            }
        }

        holder.root.onClick {
            // do stuff
        }
    }}

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) = Unit
}
