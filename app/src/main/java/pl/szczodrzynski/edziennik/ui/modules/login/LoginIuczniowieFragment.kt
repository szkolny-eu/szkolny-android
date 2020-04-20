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
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_IDZIENNIK_WEB_INVALID_SCHOOL_NAME
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_IDZIENNIK
import pl.szczodrzynski.edziennik.databinding.FragmentLoginIuczniowieBinding
import java.util.*
import kotlin.coroutines.CoroutineContext

class LoginIuczniowieFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginIuczniowieFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: FragmentLoginIuczniowieBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = FragmentLoginIuczniowieBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity.lastError?.let { error ->
            activity.lastError = null
            startCoroutineTimer(delayMillis = 100) {
                when (error.errorCode) {
                    ERROR_LOGIN_IDZIENNIK_WEB_INVALID_SCHOOL_NAME ->
                        b.loginSchoolNameLayout.error = getString(R.string.login_error_incorrect_school_name)
                    ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED ->
                        b.loginPasswordLayout.error = getString(R.string.login_error_incorrect_login_or_password)
                }
            }
        }

        b.helpButton.onClick { nav.navigate(R.id.loginIuczniowieHelpFragment, null, LoginActivity.navOptions) }
        b.backButton.onClick { nav.navigateUp() }

        b.loginButton.onClick {
            var errors = false

            b.loginSchoolNameLayout.error = null
            b.loginUsernameLayout.error = null
            b.loginPasswordLayout.error = null

            val schoolName = b.loginSchoolName.text?.toString()?.toLowerCase(Locale.ROOT) ?: ""
            val username = b.loginUsername.text?.toString()?.toLowerCase(Locale.ROOT) ?: ""
            val password = b.loginPassword.text?.toString() ?: ""

            if (schoolName.isBlank()) {
                b.loginSchoolNameLayout.error = getString(R.string.login_error_no_school_name)
                errors = true
            }
            if (username.isBlank()) {
                b.loginUsernameLayout.error = getString(R.string.login_error_no_username)
                errors = true
            }
            if (password.isBlank()) {
                b.loginPasswordLayout.error = getString(R.string.login_error_no_password)
                errors = true
            }
            if (errors) return@onClick

            errors = false

            b.loginSchoolName.setText(schoolName)
            b.loginUsername.setText(username)
            if (!"[a-z0-9_\\-]+".toRegex().matches(schoolName)) {
                b.loginSchoolNameLayout.error = getString(R.string.login_error_incorrect_school_name)
                errors = true
            }
            if (!"[a-z0-9_\\-]+".toRegex().matches(username)) {
                b.loginUsernameLayout.error = getString(R.string.login_error_incorrect_username)
                errors = true
            }
            if (errors) return@onClick

            val args = Bundle(
                    "loginType" to LOGIN_TYPE_IDZIENNIK,
                    "schoolName" to schoolName,
                    "username" to username,
                    "password" to password
            )
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions)
        }
    }
}
