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
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.databinding.FragmentLoginMobidziennikBinding
import java.util.*
import kotlin.coroutines.CoroutineContext

class LoginMobidziennikFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginMobidziennikFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: FragmentLoginMobidziennikBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = FragmentLoginMobidziennikBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity.lastError?.let { error ->
            activity.lastError = null
            startCoroutineTimer(delayMillis = 100) {
                when (error.errorCode) {
                    ERROR_LOGIN_MOBIDZIENNIK_WEB_INVALID_LOGIN ->
                        b.loginPasswordLayout.error = getString(R.string.login_error_incorrect_login_or_password)
                    ERROR_LOGIN_MOBIDZIENNIK_WEB_OLD_PASSWORD ->
                        b.loginPasswordLayout.error = getString(R.string.login_error_old_password)
                    ERROR_LOGIN_MOBIDZIENNIK_WEB_ARCHIVED ->
                        b.loginUsernameLayout.error = getString(R.string.sync_error_archived)
                    ERROR_LOGIN_MOBIDZIENNIK_WEB_INVALID_ADDRESS ->
                        b.loginServerAddressLayout.error = getString(R.string.login_error_incorrect_address)
                }
            }
        }

        b.helpButton.onClick { nav.navigate(R.id.loginMobidziennikHelpFragment, null, LoginActivity.navOptions) }
        b.backButton.onClick { nav.navigateUp() }

        b.loginButton.onClick {
            var errors = false

            b.loginServerAddressLayout.error = null
            b.loginUsernameLayout.error = null
            b.loginPasswordLayout.error = null

            val serverName = b.loginServerAddress.text
                    ?.toString()
                    ?.toLowerCase(Locale.ROOT)
                    ?.replace("(?:http://|www.|mobidziennik\\.pl|wizja\\.net|\\.)".toRegex(), "") ?: ""
            val username = b.loginUsername.text?.toString()?.toLowerCase(Locale.ROOT) ?: ""
            val password = b.loginPassword.text?.toString() ?: ""

            if (serverName.isBlank()) {
                b.loginServerAddressLayout.error = getString(R.string.login_error_no_address)
                errors = true
            }
            if (username.isBlank()) {
                b.loginUsernameLayout.error = getString(R.string.login_error_no_login)
                errors = true
            }
            if (password.isBlank()) {
                b.loginPasswordLayout.error = getString(R.string.login_error_no_password)
                errors = true
            }
            if (errors) return@onClick

            errors = false

            b.loginServerAddress.setText(serverName)
            b.loginUsername.setText(username)
            if (!"^[a-z0-9_\\-]+$".toRegex().matches(serverName)) {
                b.loginServerAddressLayout.error = getString(R.string.login_error_incorrect_address)
                errors = true
            }
            if (!"^[a-z0-9_\\-@+.]+$".toRegex().matches(username)) {
                b.loginUsernameLayout.error = getString(R.string.login_error_incorrect_login)
                errors = true
            }
            if (errors) return@onClick

            val args = Bundle(
                    "loginType" to LOGIN_TYPE_MOBIDZIENNIK,
                    "serverName" to serverName,
                    "username" to username,
                    "password" to password
            )
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions)
        }
    }
}