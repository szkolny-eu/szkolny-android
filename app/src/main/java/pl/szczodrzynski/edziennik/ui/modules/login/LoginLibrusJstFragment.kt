/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-3.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_LIBRUS_API_INVALID_LOGIN
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_LIBRUS_API_INVALID_REQUEST
import pl.szczodrzynski.edziennik.data.api.LOGIN_MODE_LIBRUS_JST
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_LIBRUS
import pl.szczodrzynski.edziennik.databinding.FragmentLoginLibrusJstBinding
import pl.szczodrzynski.edziennik.ui.dialogs.QrScannerDialog
import java.util.*
import kotlin.coroutines.CoroutineContext

class LoginLibrusJstFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginLibrusJstFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: FragmentLoginLibrusJstBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = FragmentLoginLibrusJstBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity.lastError?.let { error ->
            activity.lastError = null
            startCoroutineTimer(delayMillis = 100) {
                when (error.errorCode) {
                    ERROR_LOGIN_LIBRUS_API_INVALID_LOGIN,
                    ERROR_LOGIN_LIBRUS_API_INVALID_REQUEST ->
                        b.loginCodeLayout.error = getString(R.string.login_error_incorrect_code_or_pin)
                }
            }
        }

        b.loginQrScan.setImageDrawable(IconicsDrawable(activity)
                .icon(CommunityMaterial.Icon2.cmd_qrcode_scan)
                .colorInt(Color.BLACK)
                .sizeDp(72))
        b.loginQrScan.onClick {
            QrScannerDialog(activity, { code ->
                b.loginCode.setText(code)
                if (b.loginPin.requestFocus()) {
                    activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            })
        }

        b.helpButton.onClick { nav.navigate(R.id.loginLibrusHelpFragment, null, LoginActivity.navOptions) }
        b.backButton.onClick { nav.navigateUp() }

        b.loginButton.onClick {
            var errors = false

            b.loginCodeLayout.error = null
            b.loginPinLayout.error = null

            val code = b.loginCode.text?.toString()?.toUpperCase(Locale.ROOT) ?: ""
            val pin = b.loginPin.text?.toString() ?: ""

            if (code.isBlank()) {
                b.loginCodeLayout.error = getString(R.string.login_error_no_code)
                errors = true
            }
            if (pin.isBlank()) {
                b.loginPinLayout.error = getString(R.string.login_error_no_pin)
                errors = true
            }
            if (errors) return@onClick

            errors = false

            b.loginCode.setText(code)
            if (!"[A-Z0-9_]+".toRegex().matches(code)) {
                b.loginCodeLayout.error = getString(R.string.login_error_incorrect_code)
                errors = true
            }
            if (!"[a-z0-9_]+".toRegex().matches(pin)) {
                b.loginPinLayout.error = getString(R.string.login_error_incorrect_pin)
                errors = true
            }
            if (errors) return@onClick

            val args = Bundle(
                    "loginType" to LOGIN_TYPE_LIBRUS,
                    "loginMode" to LOGIN_MODE_LIBRUS_JST,
                    "accountCode" to code,
                    "accountPin" to pin
            )
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions)
        }
    }
}
