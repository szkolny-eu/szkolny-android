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
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.databinding.FragmentLoginVulcanBinding
import pl.szczodrzynski.edziennik.ui.dialogs.QrScannerDialog
import pl.szczodrzynski.edziennik.utils.Utils
import java.util.*
import kotlin.coroutines.CoroutineContext

class LoginVulcanFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginVulcanFragment"
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: FragmentLoginVulcanBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = FragmentLoginVulcanBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity.lastError?.let { error ->
            activity.lastError = null
            startCoroutineTimer(delayMillis = 100) {
                when (error.errorCode) {
                    ERROR_LOGIN_VULCAN_INVALID_TOKEN ->
                        b.loginTokenLayout.error = getString(R.string.login_error_incorrect_token)
                    ERROR_LOGIN_VULCAN_EXPIRED_TOKEN ->
                        b.loginTokenLayout.error = getString(R.string.login_error_expired_token)
                    ERROR_LOGIN_VULCAN_INVALID_SYMBOL ->
                        b.loginSymbolLayout.error = getString(R.string.login_error_incorrect_symbol)
                    ERROR_LOGIN_VULCAN_INVALID_PIN ->
                        b.loginPinLayout.error = getString(R.string.login_error_incorrect_pin)
                }
            }
        }

        b.loginQrScan.setImageDrawable(IconicsDrawable(activity)
                .icon(CommunityMaterial.Icon2.cmd_qrcode_scan)
                .colorInt(Color.BLACK)
                .sizeDp(72))
        b.loginQrScan.onClick {
            QrScannerDialog(activity, { code ->
                try {
                    val data = Utils.VulcanQrEncryptionUtils.decode(code)
                    "CERT#https?://.+?/([A-z]+)/mobile-api#([A-z0-9]+)#ENDCERT".toRegex().find(data)?.let {
                        b.loginToken.setText(it[2])
                        b.loginSymbol.setText(it[1])
                        if (b.loginPin.requestFocus()) {
                            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        }
                    }
                }
                catch (_: Exception) {}
            })
        }

        b.helpButton.onClick { nav.navigate(R.id.loginVulcanHelpFragment, null, LoginActivity.navOptions) }
        b.backButton.onClick { nav.navigateUp() }

        b.loginButton.onClick {
            var errors = false

            b.loginTokenLayout.error = null
            b.loginSymbolLayout.error = null
            b.loginPinLayout.error = null

            val token = b.loginToken.text?.toString()?.toUpperCase(Locale.ROOT) ?: ""
            val symbol = b.loginSymbol.text?.toString()?.toLowerCase(Locale.ROOT) ?: ""
            val pin = b.loginPin.text?.toString() ?: ""

            if (token.isBlank()) {
                b.loginTokenLayout.error = getString(R.string.login_error_no_token)
                errors = true
            }
            if (symbol.isBlank()) {
                b.loginSymbolLayout.error = getString(R.string.login_error_no_symbol)
                errors = true
            }
            if (pin.isBlank()) {
                b.loginPinLayout.error = getString(R.string.login_error_no_pin)
                errors = true
            }
            if (errors) return@onClick

            errors = false

            b.loginToken.setText(token)
            b.loginSymbol.setText(symbol)
            b.loginPin.setText(pin)
            if (!"[A-Z0-9]{5,12}".toRegex().matches(token)) {
                b.loginTokenLayout.error = getString(R.string.login_error_incorrect_token)
                errors = true
            }
            if (!"[a-z0-9_-]+".toRegex().matches(symbol)) {
                b.loginSymbolLayout.error = getString(R.string.login_error_incorrect_symbol)
                errors = true
            }
            if (!"[a-z0-9_]+".toRegex().matches(pin)) {
                b.loginPinLayout.error = getString(R.string.login_error_incorrect_pin)
                errors = true
            }
            if (errors) return@onClick

            val args = Bundle(
                    "loginType" to LOGIN_TYPE_VULCAN,
                    "deviceToken" to token,
                    "deviceSymbol" to symbol,
                    "devicePin" to pin
            )
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions)
        }
    }
}
