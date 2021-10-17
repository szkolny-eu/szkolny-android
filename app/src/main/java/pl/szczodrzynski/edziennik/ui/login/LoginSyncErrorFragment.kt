/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-14.
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
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.LoginSyncErrorFragmentBinding
import pl.szczodrzynski.edziennik.ext.onClick
import kotlin.coroutines.CoroutineContext

class LoginSyncErrorFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginSyncErrorFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginSyncErrorFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginSyncErrorFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.errorDetails.text = activity.lastError?.getStringReason(activity)
        activity.lastError = null
        b.nextButton.onClick {
            nav.navigate(R.id.loginFinishFragment, arguments, activity.navOptions)
        }
    }
}
