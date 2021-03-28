/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-16.
 */

package pl.szczodrzynski.edziennik.ui.modules.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.ui.modules.settings.cards.*
import kotlin.coroutines.CoroutineContext

class SettingsFragment : MaterialAboutFragment(), CoroutineScope {
    companion object {
        private const val TAG = "SettingsFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val util by lazy {
        SettingsUtil(activity) {
            refreshMaterialAboutList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        app = activity.application as App
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getViewTypeManager() =
        SettingsViewTypeManager()

    override fun getMaterialAboutList(activityContext: Context?): MaterialAboutList {
        return MaterialAboutList(
            SettingsProfileCard(util).card,
            SettingsThemeCard(util).card,
            SettingsSyncCard(util).card,
            SettingsRegisterCard(util).card,
            SettingsAboutCard(util).card,
        )
    }
}
