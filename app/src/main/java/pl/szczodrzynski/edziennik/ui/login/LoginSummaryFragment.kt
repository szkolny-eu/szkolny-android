/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.databinding.LoginSummaryFragmentBinding
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class LoginSummaryFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginSummaryFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginSummaryFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginSummaryFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val adapter = LoginSummaryAdapter(activity) { _ ->
            b.finishButton.isEnabled = activity.profiles.any { it.isSelected }
        }

        adapter.items = activity.profiles
        b.list.adapter = adapter
        b.list.apply {
            isNestedScrollingEnabled = false
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SimpleDividerItemDecoration(context))
        }

        b.registerMeSwitch.onChange { _, isChecked ->
            if (isChecked)
                return@onChange
            MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.login_summary_unregister_title)
                    .setMessage(R.string.login_summary_unregister_text)
                    .setPositiveButton(R.string.ok, null)
                    .setNegativeButton(R.string.cancel) { _, _ -> b.registerMeSwitch.isChecked = true }
                    .show()
        }

        b.anotherButton.onClick {
            nav.navigate(R.id.loginChooserFragment, null, activity.navOptions)
        }

        b.finishButton.onClick {
            val args = Bundle(
                    "registrationAllowed" to b.registerMeSwitch.isChecked
            )
            nav.navigate(R.id.loginSyncFragment, args, activity.navOptions)
        }
    }
}
