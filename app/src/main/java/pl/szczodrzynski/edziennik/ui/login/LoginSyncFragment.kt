/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskAllFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskErrorEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskProgressEvent
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskStartedEvent
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.LoginSyncFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.asBoldSpannable
import pl.szczodrzynski.edziennik.ext.concat
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class LoginSyncFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginSyncFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginSyncFragmentBinding
    private val nav: NavController by lazy { Navigation.findNavController(activity, R.id.nav_host_fragment) }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var finishArguments: Bundle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginSyncFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        EventBus.getDefault().removeStickyEvent(ApiTaskAllFinishedEvent::class.java)
        EventBus.getDefault().removeStickyEvent(ApiTaskErrorEvent::class.java)

        val profiles = activity.profiles.filter { it.isSelected }.map { it.profile }
        val loginStores = activity.loginStores.filter { store -> profiles.any { it.loginStoreId == store.id } }

        val registrationAllowed = arguments?.getBoolean("registrationAllowed") ?: false
        profiles.forEach {
            it.registration = if (registrationAllowed)
                Profile.REGISTRATION_ENABLED
            else
                Profile.REGISTRATION_DISABLED

            app.db.eventTypeDao().addDefaultTypes(activity, it.id)
        }

        app.db.profileDao().addAll(profiles)
        app.db.loginStoreDao().addAll(loginStores)

        finishArguments = Bundle(
                "firstProfileId" to profiles.firstOrNull()?.id
        )

        val profileIds = profiles.map { it.id }
        EdziennikTask.syncProfileList(profileIds).enqueue(activity)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncStartedEvent(event: ApiTaskStartedEvent) {
        b.loginSyncSubtitle1.text = listOf(
                getString(R.string.login_sync_subtitle_1_format),
                event.profile?.name?.asBoldSpannable()
        ).concat(" ")
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onSyncFinishedEvent(event: ApiTaskAllFinishedEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        nav.navigate(R.id.loginFinishFragment, finishArguments, activity.navOptions)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncProgressEvent(event: ApiTaskProgressEvent) {
        b.loginSyncProgressBar.progress = event.progress.roundToInt()
        b.loginSyncProgressBar.isIndeterminate = event.progress <= 0f
        b.loginSyncSubtitle2.text = event.progressText
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onSyncErrorEvent(event: ApiTaskErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        activity.error(event.error)
        nav.navigate(R.id.loginSyncErrorFragment, finishArguments, activity.navOptions)
    }

    override fun onStart() {
        EventBus.getDefault().register(this)
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
}
