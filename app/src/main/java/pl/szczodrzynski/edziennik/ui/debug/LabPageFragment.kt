/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.debug

import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.sqlite.db.SimpleSQLiteQuery
import com.chuckerteam.chucker.api.Chucker
import com.chuckerteam.chucker.api.Chucker.SCREEN_HTTP
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.config.Config
import pl.szczodrzynski.edziennik.databinding.LabFragmentBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.ui.dialogs.ProfileRemoveDialog
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.fslogin.decode
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

class LabPageFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "LabPageFragment"
    }

    private lateinit var app: App
    private lateinit var activity: AppCompatActivity
    private lateinit var b: LabFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as AppCompatActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LabFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean {
        b.app = app

        if (app.profile.id == 0) {
            b.last10unseen.isVisible = false
            b.fullSync.isVisible = false
            b.clearProfile.isVisible = false
            b.rodo.isVisible = false
            b.removeHomework.isVisible = false
            b.unarchive.isVisible = false
            b.profile.isVisible = false
        }

        b.last10unseen.onClick {
            launch(Dispatchers.Default) {
                val events = app.db.eventDao().getAllNow(App.profileId)
                val ids = events.sortedBy { it.date }.filter { it.isHomework }.takeLast(10)
                ids.forEach {
                    app.db.metadataDao().setSeen(App.profileId, it, false)
                }
            }
        }

        b.rodo.onClick {
            app.db.teacherDao().query(SimpleSQLiteQuery("UPDATE teachers SET teacherSurname = \"\" WHERE profileId = ${App.profileId}"))
        }
        
        b.fullSync.onClick { 
            app.db.query(SimpleSQLiteQuery("UPDATE profiles SET empty = 1 WHERE profileId = ${App.profileId}"))
            app.db.query(SimpleSQLiteQuery("DELETE FROM endpointTimers WHERE profileId = ${App.profileId}"))
        }

        b.clearProfile.onClick {
            ProfileRemoveDialog(activity, App.profileId, "FAKE", noProfileRemoval = true).show()
        }

        b.removeHomework.onClick {
            app.db.eventDao().getRawNow("UPDATE events SET homeworkBody = NULL WHERE profileId = ${App.profileId}")
        }

        b.chucker.isChecked = App.enableChucker
        b.chucker.onChange { _, isChecked ->
            app.config.enableChucker = isChecked
            App.enableChucker = isChecked
            MaterialAlertDialogBuilder(activity)
                .setTitle("Restart")
                .setMessage("Wymagany restart aplikacji")
                .setPositiveButton(R.string.ok) { _, _ ->
                    Process.killProcess(Process.myPid())
                    Runtime.getRuntime().exit(0)
                    exitProcess(0)
                }
                .setCancelable(false)
                .show()
        }

        if (App.enableChucker) {
            b.openChucker.isVisible = true
            b.openChucker.onClick {
                startActivity(Chucker.getLaunchIntent(activity, SCREEN_HTTP))
            }
        }

        b.disableDebug.onClick {
            app.config.devMode = false
            App.devMode = false
            MaterialAlertDialogBuilder(activity)
                .setTitle("Restart")
                .setMessage("Wymagany restart aplikacji")
                .setPositiveButton(R.string.ok) { _, _ ->
                    Process.killProcess(Process.myPid())
                    Runtime.getRuntime().exit(0)
                    exitProcess(0)
                }
                .setCancelable(false)
                .show()
        }

        b.unarchive.onClick {
            app.profile.archived = false
            app.profile.archiveId = null
            app.profileSave()
        }

        b.resetCert.onClick {
            app.config.apiInvalidCert = null
        }

        b.rebuildConfig.onClick {
            App.config = Config(App.db)
        }

        val profiles = app.db.profileDao().allNow
        b.profile.clear()
        b.profile += profiles.map { TextInputDropDown.Item(it.id.toLong(), "${it.id} ${it.name} archived ${it.archived}", tag = it) }
        b.profile.select(app.profileId.toLong())
        b.profile.setOnChangeListener {
            if (activity is MainActivity)
                (activity as MainActivity).loadProfile(it.id.toInt())
            return@setOnChangeListener true
        }

        val colorSecondary = android.R.attr.textColorSecondary.resolveAttr(activity)
        startCoroutineTimer(500L, 300L) {
            val text = app.cookieJar.sessionCookies
                    .map { it.cookie }
                    .sortedBy { it.domain() }
                    .groupBy { it.domain() }
                    .map {
                        listOf(
                                it.key.asBoldSpannable(),
                                ":\n",
                                it.value
                                        .sortedBy { it.name() }
                                        .map {
                                            listOf(
                                                    "    ",
                                                    it.name(),
                                                    "=",
                                                    it.value().decode().take(40).asItalicSpannable().asColoredSpannable(colorSecondary)
                                            ).concat("")
                                        }.concat("\n")
                        ).concat("")
                    }.concat("\n\n")
            b.cookies.text = text
        }

        return true
    }
}
