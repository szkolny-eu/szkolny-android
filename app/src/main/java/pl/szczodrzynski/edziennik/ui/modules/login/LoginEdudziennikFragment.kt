/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_EDUDZIENNIK_WEB_INVALID_LOGIN
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_EDUDZIENNIK
import pl.szczodrzynski.edziennik.databinding.FragmentLoginEdudziennikBinding
import pl.szczodrzynski.edziennik.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar
import kotlin.coroutines.CoroutineContext

class LoginEdudziennikFragment : Fragment(), CoroutineScope {

    private val app by lazy { activity?.application as App? }

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var b: FragmentLoginEdudziennikBinding

    private lateinit var nav: NavController
    private lateinit var errorSnackbar: ErrorSnackbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.also { activity ->
            nav = Navigation.findNavController(activity, R.id.nav_host_fragment)
            errorSnackbar = (activity as LoginActivity).errorSnackbar
        }

        b = FragmentLoginEdudziennikBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { launch {
        startCoroutineTimer(delayMillis = 100) {
            val error = LoginActivity.error

            if (error != null) {
                when (error.errorCode) {
                    ERROR_LOGIN_EDUDZIENNIK_WEB_INVALID_LOGIN ->
                        b.loginPasswordLayout.error = getString(R.string.login_error_incorrect_login_or_password)
                }

                errorSnackbar.addError(error)
                LoginActivity.error = null
            }
        }

        b.backButton.setOnClickListener { nav.navigateUp() }
        b.loginButton.setOnClickListener { login() }
    }}

    private fun login() {
        var errors = false

        b.loginEmailLayout.error = null
        b.loginPasswordLayout.error = null

        val emailEditable = b.loginEmail.text
        val passwordEditable = b.loginPassword.text

        if (emailEditable.isNullOrBlank()) {
            b.loginEmailLayout.error = getString(R.string.login_error_no_email)
            errors = true
        }

        if (passwordEditable.isNullOrBlank()) {
            b.loginPasswordLayout.error = getString(R.string.login_error_no_password)
            errors = true
        }

        if (errors)
            return

        nav.navigate(R.id.loginProgressFragment, Bundle().apply {
            putInt("loginType", LOGIN_TYPE_EDUDZIENNIK)
            putString("email", emailEditable.toString())
            putString("password", passwordEditable.toString())
        }, LoginActivity.navOptions)
    }
}
