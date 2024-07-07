/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.debug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.sqlite.db.SimpleSQLiteQuery
import com.chuckerteam.chucker.api.Chucker
import com.chuckerteam.chucker.api.Chucker.SCREEN_HTTP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.SignatureInterceptor
import pl.szczodrzynski.edziennik.data.config.Config
import pl.szczodrzynski.edziennik.data.db.entity.EventType.Companion.SOURCE_DEFAULT
import pl.szczodrzynski.edziennik.databinding.LabFragmentBinding
import pl.szczodrzynski.edziennik.ext.asBoldSpannable
import pl.szczodrzynski.edziennik.ext.asColoredSpannable
import pl.szczodrzynski.edziennik.ext.asItalicSpannable
import pl.szczodrzynski.edziennik.ext.asUnderlineSpannable
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ext.takeValue
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.ProfileRemoveDialog
import pl.szczodrzynski.edziennik.ui.dialogs.RestartDialog
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.fslogin.decode

class LabPageFragment : BaseFragment<LabFragmentBinding, AppCompatActivity>(
    inflater = LabFragmentBinding::inflate,
) {

    override fun getRefreshScrollingView() = b.scrollView

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.app = app

        if (app.profile.id == 0) {
            b.last10unseen.isVisible = false
            b.fullSync.isVisible = false
            b.clearProfile.isVisible = false
            b.clearEndpointTimers.isVisible = false
            b.rodo.isVisible = false
            b.removeHomework.isVisible = false
            b.resetEventTypes.isVisible = false
            b.unarchive.isVisible = false
            b.profile.isVisible = false
            b.clearConfigProfile.isVisible = false
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
            app.profile.empty = true
            app.profileSave()
        }

        b.clearProfile.onClick {
            ProfileRemoveDialog(activity, App.profileId, "FAKE", noProfileRemoval = true).show()
        }

        b.clearEndpointTimers.onClick {
            app.db.endpointTimerDao().clear(app.profileId)
        }

        b.removeHomework.onClick {
            app.db.eventDao().getRawNow("UPDATE events SET homeworkBody = NULL WHERE profileId = ${App.profileId}")
        }

        b.resetEventTypes.onClick {
            app.db.eventTypeDao().clearBySource(App.profileId, SOURCE_DEFAULT)
            app.db.eventTypeDao().getAllWithDefaults(App.profile)
        }

        b.chucker.isChecked = App.enableChucker
        b.chucker.onChange { _, isChecked ->
            app.config.enableChucker = isChecked
            App.enableChucker = isChecked
            RestartDialog(activity).show()
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
            RestartDialog(activity).show()
        }

        b.unarchive.onClick {
            app.profile.archived = false
            app.profile.archiveId = null
            app.profileSave()
        }

        b.resetCert.onClick {
            app.config.apiInvalidCert = null
        }

        b.apiKey.setText(app.config.apiKeyCustom ?: SignatureInterceptor.API_KEY)
        b.apiKey.doAfterTextChanged {
            it?.toString()?.let { key ->
                if (key == SignatureInterceptor.API_KEY)
                    app.config.apiKeyCustom = null
                else
                    app.config.apiKeyCustom = key.takeValue()?.trim()
            }
        }

        b.clearConfigProfile.onClick {
            app.db.configDao().clear(app.profileId)
        }
        b.clearConfigGlobal.onClick {
            app.db.configDao().clear(-1)
        }
        b.rebuildConfig.onClick {
            App.config = Config(app)
            App.config.migrate()
        }

        val profiles = app.db.profileDao().allNow
        b.profile.clear()
        b.profile += profiles.map { TextInputDropDown.Item(it.id.toLong(), "${it.id} ${it.name} archived ${it.archived}", tag = it) }
        b.profile.select(app.profileId.toLong())
        b.profile.setOnChangeListener {
            if (activity is MainActivity)
                (activity as MainActivity).navigate(profileId = it.id.toInt())
            return@setOnChangeListener true
        }

        b.clearCookies.onClick {
            app.cookieJar.clearAllDomains()
        }

        val colorSecondary = android.R.attr.textColorSecondary.resolveAttr(activity)
        startCoroutineTimer(500L, 300L) {
            val text = app.cookieJar.getAllDomains()
                .sortedBy { it.domain() }
                .groupBy { it.domain() }
                .map { pair ->
                    listOf(
                        pair.key.asBoldSpannable(),
                        ":\n",
                        pair.value
                            .sortedBy { it.name() }
                            .map { cookie ->
                                listOf(
                                    "    ",
                                    if (cookie.persistent())
                                        cookie.name()
                                            .asUnderlineSpannable()
                                    else
                                        cookie.name(),
                                    "=",
                                    cookie.value()
                                        .decode()
                                        .take(40)
                                        .asItalicSpannable()
                                        .asColoredSpannable(colorSecondary),
                                ).concat("")
                            }.concat("\n")
                    ).concat("")
                }.concat("\n\n")
            b.cookies.text = text
        }
    }
}
