/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.core.manager.UserActionManager
import pl.szczodrzynski.edziennik.data.api.ERROR_REQUIRES_USER_ACTION
import pl.szczodrzynski.edziennik.data.api.LOGIN_NO_ARGUMENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskErrorEvent
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.databinding.LoginProgressFragmentBinding
import pl.szczodrzynski.edziennik.ext.getEnum
import pl.szczodrzynski.edziennik.ext.joinNotNullStrings
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

class LoginProgressFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginProgressFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginProgressFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginProgressFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        EventBus.getDefault().removeStickyEvent(FirstLoginFinishedEvent::class.java)

        val args = arguments ?: run {
            activity.error(ApiError(TAG, LOGIN_NO_ARGUMENTS))
            nav.navigateUp()
            return
        }

        doFirstLogin(args)
    }

    private fun doFirstLogin(args: Bundle) {
        launch {
            activity.errorSnackbar.dismiss()

            val maxProfileId = max(
                    app.db.profileDao().lastId ?: 0,
                    activity.profiles.maxByOrNull { it.profile.id }?.profile?.id ?: 0
            )
            val loginType = args.getEnum<LoginType>("loginType") ?: return@launch
            val loginMode = args.getEnum<LoginMode>("loginMode") ?: return@launch

            val loginStore = LoginStore(
                    id = maxProfileId + 1,
                    type = loginType,
                    mode = loginMode
            )
            loginStore.copyFrom(args)
            loginStore.removeLoginData("loginType")
            loginStore.removeLoginData("loginMode")
            EdziennikTask.firstLogin(loginStore).enqueue(activity)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onFirstLoginFinishedEvent(event: FirstLoginFinishedEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.profileList.isEmpty()) {
            SimpleDialog<Unit>(activity) {
                title(R.string.login_account_no_students)
                message(R.string.login_account_no_students_text)
                positive(R.string.ok) {
                    nav.navigateUp()
                }
                cancelable(false)
            }.show()
            return
        }

        // update subnames with school years and class name
        for (profile in event.profileList) {
            val schoolYearName = "${profile.studentSchoolYearStart}/${profile.studentSchoolYearStart + 1}"
            profile.subname = joinNotNullStrings(
                    " - ",
                    profile.studentClassName,
                    schoolYearName
            )
        }

        activity.loginStores += event.loginStore
        activity.profiles += event.profileList.map { LoginSummaryAdapter.Item(it) }
        activity.errorSnackbar.dismiss()
        nav.navigate(R.id.loginSummaryFragment, null, activity.navOptions)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onSyncErrorEvent(event: ApiTaskErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        activity.error(event.error)
        nav.navigateUp()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserActionRequiredEvent(event: UserActionRequiredEvent) {
        val args = arguments ?: run {
            activity.error(ApiError(TAG, LOGIN_NO_ARGUMENTS))
            nav.navigateUp()
            return
        }

        val callback = UserActionManager.UserActionCallback(
            onSuccess = { data ->
                args.putAll(data)
                doFirstLogin(args)
            },
            onFailure = {
                activity.error(ApiError(TAG, ERROR_REQUIRES_USER_ACTION))
                nav.navigateUp()
            },
            onCancel = {
                nav.navigateUp()
            },
        )

        app.userActionManager.execute(activity, event, callback)
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
