/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-3.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_TEMPLATE
import pl.szczodrzynski.edziennik.databinding.FragmentLoginTemplateBinding
import java.util.*
import kotlin.coroutines.CoroutineContext

class LoginTemplateFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginTemplateFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: FragmentLoginTemplateBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = FragmentLoginTemplateBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity.lastError?.let { error ->
            activity.lastError = null
            startCoroutineTimer(delayMillis = 100) {
                when (error.errorCode) {
                    ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN ->
                        b.loginPasswordLayout.error = getString(R.string.login_error_incorrect_login_or_password)
                    ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED ->
                        b.loginEmailLayout.error = getString(R.string.login_error_account_not_activated)
                }
            }
        }

        b.helpButton.onClick { nav.navigate(R.id.loginLibrusHelpFragment, null, LoginActivity.navOptions) }
        b.backButton.onClick { nav.navigateUp() }

        b.loginButton.onClick {
            var errors = false

            b.loginEmailLayout.error = null
            b.loginPasswordLayout.error = null

            val email = b.loginEmail.text?.toString()?.toLowerCase(Locale.ROOT) ?: ""
            val password = b.loginPassword.text?.toString() ?: ""

            if (email.isBlank()) {
                b.loginEmailLayout.error = getString(R.string.login_error_no_email)
                errors = true
            }
            if (password.isBlank()) {
                b.loginPasswordLayout.error = getString(R.string.login_error_no_password)
                errors = true
            }
            if (errors) return@onClick

            errors = false

            b.loginEmail.setText(email)
            if (!"([\\w.\\-_+]+)?\\w+@[\\w-_]+(\\.\\w+)+".toRegex().matches(email)) {
                b.loginEmailLayout.error = getString(R.string.login_error_incorrect_email)
                errors = true
            }
            if (errors) return@onClick

            val args = Bundle(
                    "loginType" to LOGIN_TYPE_TEMPLATE,
                    "email" to email,
                    "password" to password
            )
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions)
        }
    }
}
