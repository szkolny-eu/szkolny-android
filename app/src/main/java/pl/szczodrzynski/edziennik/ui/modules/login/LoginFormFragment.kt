/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.databinding.LoginFormCheckboxItemBinding
import pl.szczodrzynski.edziennik.databinding.LoginFormFieldItemBinding
import pl.szczodrzynski.edziennik.databinding.LoginFormFragmentBinding
import pl.szczodrzynski.navlib.colorAttr
import java.util.*
import kotlin.coroutines.CoroutineContext

class LoginFormFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginFormFragment"
        // eggs
        var wantEggs = false
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginFormFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginFormFragmentBinding.inflate(inflater)
        return b.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return
        b.backButton.onClick { nav.navigateUp() }

        b.errorLayout.isVisible = false
        b.errorLayout.background?.setTintColor(R.attr.colorError.resolveAttr(activity))

        val loginType = arguments?.getInt("loginType") ?: return
        val register = LoginInfo.list.firstOrNull { it.loginType == loginType } ?: return
        val loginMode = arguments?.getInt("loginMode") ?: return
        val mode = register.loginModes.firstOrNull { it.loginMode == loginMode } ?: return

        val platformName = arguments?.getString("platformName")
        val platformGuideText = arguments?.getString("platformGuideText")
        val platformDescription = arguments?.getString("platformDescription")
        val platformFormFields = arguments?.getString("platformFormFields")?.split(";")
        val platformRealmData = arguments?.getString("platformRealmData")?.toJsonObject()

        b.title.setText(R.string.login_form_title_format, app.getString(register.registerName))
        b.subTitle.text = platformName ?: app.getString(mode.name)
        b.text.text = platformGuideText ?: app.getString(mode.guideText)

        val credentials = mutableMapOf<LoginInfo.BaseCredential, Any>()

        for (credential in mode.credentials) {
            if (platformFormFields?.contains(credential.keyName) == false)
                continue

            if (credential is LoginInfo.FormField) {
                val b = LoginFormFieldItemBinding.inflate(layoutInflater)
                b.textLayout.hint = app.getString(credential.name)
                if (credential.hideText) {
                    b.textEdit.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                    b.textLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                }
                b.textEdit.addTextChangedListener {
                    b.textLayout.error = null
                }

                b.textEdit.id = credential.name

                b.textEdit.setText(arguments?.getString(credential.keyName) ?: "")
                b.textLayout.startIconDrawable = IconicsDrawable(activity)
                        .icon(credential.icon)
                        .sizeDp(24)
                        .paddingDp(2)
                        .colorAttr(activity, R.attr.colorOnBackground)

                this.b.formContainer.addView(b.root)
                credentials[credential] = b
            }
            if (credential is LoginInfo.FormCheckbox) {
                val b = LoginFormCheckboxItemBinding.inflate(layoutInflater)
                b.checkbox.text = app.getString(credential.name)
                b.checkbox.onChange { _, isChecked ->
                    b.errorText.text = null

                    // eggs
                    if (register.internalName == "podlasie") {
                        wantEggs = !isChecked
                    }
                }
                if (arguments?.containsKey(credential.keyName) == true) {
                    b.checkbox.isChecked = arguments?.getBoolean(credential.keyName) == true
                }

                this.b.formContainer.addView(b.root)
                credentials[credential] = b
            }
        }

        activity.lastError?.let { error ->
            activity.lastError = null
            startCoroutineTimer(delayMillis = 200L) {
                for (credential in credentials) {
                    credential.key.errorCodes[error.errorCode]?.let {
                        (credential.value as? LoginFormFieldItemBinding)?.let { b ->
                            b.textLayout.error = app.getString(it)
                        }
                        (credential.value as? LoginFormCheckboxItemBinding)?.let { b ->
                            b.errorText.text = app.getString(it)
                        }
                        return@startCoroutineTimer
                    }
                }
                mode.errorCodes[error.errorCode]?.let {
                    b.errorText.text = app.getString(it)
                    b.errorLayout.isVisible = true
                    return@startCoroutineTimer
                }
            }
        }

        b.loginButton.onClick {
            val payload = Bundle(
                    "loginType" to loginType,
                    "loginMode" to loginMode
            )

            if (App.debugMode && b.fakeLogin.isChecked) {
                payload.putBoolean("fakeLogin", true)
            }

            payload.putBundle("webRealmData", platformRealmData?.toBundle())

            var hasErrors = false
            credentials.forEach { (credential, b) ->
                if (credential is LoginInfo.FormField && b is LoginFormFieldItemBinding) {
                    var text = b.textEdit.text?.toString() ?: return@forEach
                    if (!credential.hideText)
                        text = text.trim()

                    if (credential.caseMode == LoginInfo.FormField.CaseMode.UPPER_CASE)
                        text = text.toUpperCase(Locale.getDefault())
                    if (credential.caseMode == LoginInfo.FormField.CaseMode.LOWER_CASE)
                        text = text.toLowerCase(Locale.getDefault())

                    credential.stripTextRegex?.let {
                        text = text.replace(it.toRegex(), "")
                    }

                    b.textEdit.setText(text)

                    if (credential.isRequired && text.isBlank()) {
                        b.textLayout.error = app.getString(credential.emptyText)
                        hasErrors = true
                        return@forEach
                    }

                    if (!text.matches(credential.validationRegex.toRegex())) {
                        b.textLayout.error = app.getString(credential.invalidText)
                        hasErrors = true
                        return@forEach
                    }

                    payload.putString(credential.keyName, text)
                    arguments?.putString(credential.keyName, text)
                }
                if (credential is LoginInfo.FormCheckbox && b is LoginFormCheckboxItemBinding) {
                    val checked = b.checkbox.isChecked
                    payload.putBoolean(credential.keyName, checked)
                    arguments?.putBoolean(credential.keyName, checked)
                }
            }

            if (hasErrors)
                return@onClick

            nav.navigate(R.id.loginProgressFragment, payload, activity.navOptions)
        }
    }
}
