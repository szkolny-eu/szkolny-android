/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-3.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.os.Bundle
import android.text.Html
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
import pl.szczodrzynski.edziennik.databinding.FragmentLoginSummaryBinding
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class LoginSummaryFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginSummaryFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: FragmentLoginSummaryBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = FragmentLoginSummaryBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        b.profileListView.apply {
            adapter = LoginSummaryProfileAdapter(activity, activity.profiles) { item ->
                b.finishButton.isEnabled = activity.profiles.any { it.isSelected }
            }
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
            nav.navigate(R.id.loginChooserFragment, null, LoginActivity.navOptions)
        }

        b.finishButton.onClick {
            if (!app.config.privacyPolicyAccepted) {
                MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.privacy_policy)
                        .setMessage(Html.fromHtml("Korzystając z aplikacji potwierdzasz <a href=\"http://szkolny.eu/privacy-policy\">przeczytanie Polityki prywatności</a> i akceptujesz jej postanowienia."))
                        .setPositiveButton(R.string.i_agree) { _, _ ->
                            app.config.privacyPolicyAccepted = true
                            b.finishButton.performClick()
                        }
                        .setNegativeButton(R.string.i_disagree, null)
                        .show()
                return@onClick
            }

            val args = Bundle(
                    "registrationAllowed" to b.registerMeSwitch.isChecked
            )
            nav.navigate(R.id.loginSyncFragment, args, LoginActivity.navOptions)
        }
    }
}