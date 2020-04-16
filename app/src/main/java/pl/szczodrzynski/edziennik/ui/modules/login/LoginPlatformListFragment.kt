/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.databinding.LoginPlatformListFragmentBinding
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class LoginPlatformListFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginPlatformListFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginPlatformListFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here
    private val api by lazy { SzkolnyApi(app) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginPlatformListFragmentBinding.inflate(inflater)
        return b.root
    }

    private lateinit var timeoutJob: Job

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return
        b.backButton.onClick { nav.navigateUp() }

        val loginType = arguments?.getInt("loginType") ?: return
        val register = LoginInfo.list.firstOrNull { it.loginType == loginType } ?: return
        val loginMode = arguments?.getInt("loginMode") ?: return
        val mode = register.loginModes.firstOrNull { it.loginMode == loginMode } ?: return

        timeoutJob = startCoroutineTimer(5000L) {
            b.timeoutText.isVisible = true
            timeoutJob.cancel()
        }

        val adapter = LoginPlatformAdapter(activity) { platform ->
            nav.navigate(R.id.loginFormFragment, Bundle(
                    "loginType" to platform.loginType,
                    "loginMode" to platform.loginMode,
                    "platformName" to platform.name,
                    "platformDescription" to platform.description,
                    "platformFormFields" to platform.formFields.joinToString(";"),
                    "platformApiData" to platform.apiData.toString()
            ), activity.navOptions)
        }

        launch {
            val platforms = LoginInfo.platformList[mode.name]
                    ?: run {
                        api.runCatching(activity) {
                            getPlatforms(register.internalName)
                        } ?: run {
                            nav.navigateUp()
                            return@launch
                        }
                    }
            LoginInfo.platformList[mode.name] = platforms

            adapter.items = platforms
            b.list.adapter = adapter
            b.list.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(SimpleDividerItemDecoration(context))
            }
            timeoutJob.cancel()
            b.loadingLayout.isVisible = false
            b.list.isVisible = true
        }
    }
}
