/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-3.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.LOGIN_NO_ARGUMENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskErrorEvent
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.databinding.FragmentLoginProgressBinding
import kotlin.coroutines.CoroutineContext

class LoginProgressFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginProgressFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: FragmentLoginProgressBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = FragmentLoginProgressBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = arguments ?: run {
            activity.error(ApiError(TAG, LOGIN_NO_ARGUMENTS))
            nav.navigateUp()
            return
        }

        launch {
            val firstProfileId = (app.db.profileDao().lastId ?: 0) + 1
            val loginType = args.getInt("loginType", -1)
            val loginMode = args.getInt("loginMode", 0)

            val loginStore = LoginStore(
                    id = firstProfileId,
                    type = loginType,
                    mode = loginMode
            )
            loginStore.copyFrom(args)
            if (App.devMode && LoginChooserFragment.fakeLogin) {
                loginStore.putLoginData("fakeLogin", true)
            }
            EdziennikTask.firstLogin(loginStore).enqueue(activity)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFirstLoginFinishedEvent(event: FirstLoginFinishedEvent) {
        if (event.profileList.isEmpty()) {
            MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.login_account_no_students)
                    .setMessage(R.string.login_account_no_students_text)
                    .setPositiveButton(R.string.ok, null)
                    .setOnDismissListener { nav.navigateUp() }
                    .show()
            return
        }
        activity.loginStores += event.loginStore
        activity.profiles += event.profileList.map { LoginSummaryProfileAdapter.Item(it) }
        nav.navigate(R.id.loginSummaryFragment, null, LoginActivity.navOptions)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onSyncErrorEvent(event: ApiTaskErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        activity.error(event.error)
        nav.navigateUp()
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
