/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-22.
 */

package pl.szczodrzynski.edziennik.ui.modules.home.cards

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
import pl.szczodrzynski.edziennik.dp
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCard
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment
import pl.szczodrzynski.edziennik.ui.modules.login.LoginLibrusCaptchaActivity
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

        b.composeNewButton.onClick {
            activity.loadTarget(MainActivity.TARGET_MESSAGES_COMPOSE)
        }

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
            EdziennikTask.recipientListGet(App.profileId).enqueue(activity)
        }


        b.pruneWorkButton.onClick {
            WorkManager.getInstance(app).pruneWork()
        }

        b.runChucker.onClick {
            app.startActivity(Chucker.getLaunchIntent(activity, 1));
        }

        b.librusCaptchaButton.onClick {
            app.startActivity(Intent(activity, LoginLibrusCaptchaActivity::class.java))
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

        holder.root.onClick {
            // do stuff
        }
    }}

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) = Unit
}
