package pl.szczodrzynski.edziennik.ui.modules.login


import android.os.AsyncTask
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.ApiService
import pl.szczodrzynski.edziennik.api.v2.events.SyncErrorEvent
import pl.szczodrzynski.edziennik.api.v2.events.SyncFinishedEvent
import pl.szczodrzynski.edziennik.api.v2.events.SyncProgressEvent
import pl.szczodrzynski.edziennik.api.v2.events.SyncStartedEvent
import pl.szczodrzynski.edziennik.api.v2.events.requests.SyncProfileListRequest
import pl.szczodrzynski.edziennik.data.db.modules.events.Event.*
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.Companion.REGISTRATION_DISABLED
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.Companion.REGISTRATION_ENABLED
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.Companion.REGISTRATION_UNSPECIFIED
import pl.szczodrzynski.edziennik.databinding.FragmentLoginSyncBinding

class LoginSyncFragment : Fragment() {

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: FragmentLoginSyncBinding
    private val nav: NavController by lazy { Navigation.findNavController(activity, R.id.nav_host_fragment) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        b = FragmentLoginSyncBinding.inflate(inflater)
        return b.root
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncStartedEvent(event: SyncStartedEvent) {
        b.loginSyncSubtitle1.text = Html.fromHtml(getString(R.string.login_sync_subtitle_1_format, event.profile?.name ?: ""))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncFinishedEvent(event: SyncFinishedEvent) {
        nav.navigate(R.id.loginFinishFragment, null, LoginActivity.navOptions)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncProgressEvent(event: SyncProgressEvent) {
        b.loginSyncProgressBar.progress = event.progress
        b.loginSyncSubtitle2.text = event.progressRes?.let { getString(it) }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncErrorEvent(event: SyncErrorEvent) {
        LoginActivity.error = event.error.toAppError()
        nav.navigate(R.id.loginSyncErrorFragment, null, LoginActivity.navOptions)
    }

    private fun begin() {
        AsyncTask.execute {
            var profileId = app.profileLastId() + 1
            val firstProfileId = profileId
            var loginStoreId = profileId
            // profileId contains the first ID free to use

            val profileIds = mutableListOf<Int>()

            for (profileObject in LoginActivity.profileObjects) {
                for ((subIndex, profile) in profileObject.profileList.withIndex()) {
                    if (profileObject.selectedList[subIndex]) {
                        saveProfile(
                                profile,
                                profileObject.loginStore,
                                profileId,
                                loginStoreId
                        )
                        profileIds += profileId
                        profileId++
                    }
                }
                loginStoreId = profileId
            }

            /*for (profile in app.db.profileDao().allNow) {
                d(TAG, profile.toString())
            }
            for (loginStore in app.db.loginStoreDao().allNow) {
                d(TAG, loginStore.toString())
            }*/

            if (app.appConfig.loginFinished) {
                LoginFinishFragment.firstRun = false
            } else {
                LoginFinishFragment.firstRun = true
                app.appConfig.loginFinished = true
                app.saveConfig("loginFinished")
            }
            LoginFinishFragment.firstProfileId = firstProfileId

            ApiService.start(activity)
            EventBus.getDefault().postSticky(SyncProfileListRequest(profileIds))
        }
    }

    private fun saveProfile(profile: Profile, loginStore: LoginStore, profileId: Int, loginStoreId: Int) {
        profile.registration = REGISTRATION_UNSPECIFIED
        if (arguments != null) {
            if (arguments!!.getBoolean("registrationAllowed", false)) {
                profile.registration = REGISTRATION_ENABLED
            } else {
                profile.registration = REGISTRATION_DISABLED
            }
        }
        profile.id = profileId
        profile.loginStoreId = loginStoreId
        loginStore.id = loginStoreId
        val typeList = listOf(
                EventType(profileId, TYPE_HOMEWORK, getString(R.string.event_type_homework), COLOR_HOMEWORK),
                EventType(profileId, TYPE_DEFAULT, getString(R.string.event_other), COLOR_DEFAULT),
                EventType(profileId, TYPE_EXAM, getString(R.string.event_exam), COLOR_EXAM),
                EventType(profileId, TYPE_SHORT_QUIZ, getString(R.string.event_short_quiz), COLOR_SHORT_QUIZ),
                EventType(profileId, TYPE_ESSAY, getString(R.string.event_essay), COLOR_SHORT_QUIZ),
                EventType(profileId, TYPE_PROJECT, getString(R.string.event_project), COLOR_PROJECT),
                EventType(profileId, TYPE_PT_MEETING, getString(R.string.event_pt_meeting), COLOR_PT_MEETING),
                EventType(profileId, TYPE_EXCURSION, getString(R.string.event_excursion), COLOR_EXCURSION),
                EventType(profileId, TYPE_READING, getString(R.string.event_reading), COLOR_READING),
                EventType(profileId, TYPE_CLASS_EVENT, getString(R.string.event_class_event), COLOR_CLASS_EVENT),
                EventType(profileId, TYPE_INFORMATION, getString(R.string.event_information), COLOR_INFORMATION)
        )
        app.db.eventTypeDao().addAll(typeList)
        app.profileSaveFull(profile, loginStore)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded)
            return

        LoginActivity.error = null

        begin()
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