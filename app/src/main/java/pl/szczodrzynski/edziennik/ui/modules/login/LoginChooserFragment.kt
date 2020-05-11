/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.app.Activity
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
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.Bundle
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.LoginChooserFragmentBinding
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class LoginChooserFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginChooserFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginChooserFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginChooserFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        val adapter = LoginChooserAdapter(activity) { loginType, loginMode ->
            if (loginMode.isPlatformSelection) {
                nav.navigate(R.id.loginPlatformListFragment, Bundle(
                        "loginType" to loginType.loginType,
                        "loginMode" to loginMode.loginMode
                ), activity.navOptions)
                return@LoginChooserAdapter
            }

            nav.navigate(R.id.loginFormFragment, Bundle(
                    "loginType" to loginType.loginType,
                    "loginMode" to loginMode.loginMode
            ), activity.navOptions)
        }

        LoginInfo.chooserList = LoginInfo.chooserList
                ?: LoginInfo.list.toMutableList<Any>()

        adapter.items = LoginInfo.chooserList!!
        b.list.adapter = adapter
        b.list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SimpleDividerItemDecoration(context))
        }

        when {
            activity.loginStores.isNotEmpty() -> {
                // we are navigated here from LoginSummary
                b.cancelButton.isVisible = true
                b.cancelButton.onClick { nav.navigateUp() }
            }
            app.config.loginFinished -> {
                // we are navigated here from AppDrawer
                b.cancelButton.isVisible = true
                b.cancelButton.onClick {
                    activity.setResult(Activity.RESULT_CANCELED)
                    activity.finish()
                }
            }
            else -> {
                // there are no profiles
                b.cancelButton.isVisible = false
            }
        }
    }
}
