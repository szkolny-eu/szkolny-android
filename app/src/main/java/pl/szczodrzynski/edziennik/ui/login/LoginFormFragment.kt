/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.textfield.TextInputLayout
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.databinding.LoginFormCheckboxItemBinding
import pl.szczodrzynski.edziennik.databinding.LoginFormFieldItemBinding
import pl.szczodrzynski.edziennik.databinding.LoginFormFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.getEnum
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.resolveString
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ext.toBundle
import pl.szczodrzynski.edziennik.ext.toDrawable
import pl.szczodrzynski.edziennik.ext.toJsonObject
import pl.szczodrzynski.edziennik.ui.dialogs.QrScannerDialog
import pl.szczodrzynski.edziennik.ui.login.LoginInfo.BaseCredential
import pl.szczodrzynski.edziennik.ui.login.LoginInfo.FormCheckbox
import pl.szczodrzynski.edziennik.ui.login.LoginInfo.FormField
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.iterator
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

class LoginFormFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LoginFormFragment"

        // eggs
        var wantEggs = false
        var isEggs = false
    }

    private lateinit var app: App
    private lateinit var activity: LoginActivity
    private lateinit var b: LoginFormFragmentBinding
    private val nav by lazy { activity.nav }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val credentials = mutableMapOf<BaseCredential, ViewBinding>()
    private val platformName
        get() = arguments?.getString("platformName")
    private val platformGuideText
        get() = arguments?.getString("platformGuideText")
    private val platformDescription
        get() = arguments?.getString("platformDescription")
    private val platformFormFields
        get() = arguments?.getString("platformFormFields")?.split(";")
    private val platformData
        get() = arguments?.getString("platformData")?.toJsonObject()
    private val platformStoreKey
        get() = arguments?.getString("platformStoreKey")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        activity = (getActivity() as LoginActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LoginFormFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return
        b.backButton.onClick { nav.navigateUp() }

        b.errorLayout.isVisible = false
        b.errorLayout.background?.setTintColor(R.attr.colorError.resolveAttr(activity))

        val loginType = arguments?.getEnum<LoginType>("loginType") ?: return
        val register = LoginInfo.list.firstOrNull { it.loginType == loginType } ?: return
        val loginMode = arguments?.getEnum<LoginMode>("loginMode") ?: return
        val mode = register.loginModes.firstOrNull { it.loginMode == loginMode } ?: return

        if (mode.credentials.isEmpty()) {
            login(loginType, loginMode)
            return
        }

        b.title.setText(R.string.login_form_title_format, app.getString(register.registerName))
        b.subTitle.text = platformName ?: app.getString(mode.name)
        b.text.text = platformGuideText ?: app.getString(mode.guideText)

        // eggs
        isEggs = register.loginType == LoginType.PODLASIE

        for (credential in mode.credentials) {
            if (platformFormFields?.contains(credential.keyName) == false)
                continue

            val b = when (credential) {
                is FormField -> buildFormField(credential)
                is FormCheckbox -> buildFormCheckbox(credential)
                else -> continue
            }
            this.b.formContainer.addView(b.root)
            credentials[credential] = b
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
            login(loginType, loginMode)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <C : BaseCredential, B : ViewBinding> getCredential(keyName: String): Pair<C, B>? {
        val (credential, binding) = credentials.entries.firstOrNull {
            it.key.keyName == keyName
        } ?: return null
        val c = credential as? C ?: return null
        val b = binding as? B ?: return null
        return c to b
    }

    @SuppressLint("ResourceType")
    private fun buildFormField(credential: FormField): LoginFormFieldItemBinding {
        val b = LoginFormFieldItemBinding.inflate(layoutInflater)

        if (credential.isNumber) {
            b.textEdit.inputType = InputType.TYPE_CLASS_NUMBER
        }

        if (credential.qrDecoderClass != null) {
            b.textLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            b.textLayout.endIconDrawable = CommunityMaterial.Icon3.cmd_qrcode.toDrawable()
            b.textLayout.setEndIconOnClickListener {
                scanQrCode(credential)
            }
        }

        if (credential.hideText) {
            b.textEdit.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            b.textLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }

        b.textEdit.addTextChangedListener {
            b.textLayout.error = null
        }

        b.textEdit.id = credential.name
        b.textEdit.setText(arguments?.getString(credential.keyName))
        b.textLayout.hint = credential.name.resolveString(app)
        b.textLayout.prefixText = credential.prefix?.resolveString(app)
        b.textLayout.suffixText = credential.suffix?.resolveString(app)
        b.textLayout.tag = credential

        b.textLayout.startIconDrawable = credential.icon.toDrawable()

        return b
    }

    private fun buildFormCheckbox(credential: FormCheckbox): LoginFormCheckboxItemBinding {
        val b = LoginFormCheckboxItemBinding.inflate(layoutInflater)

        b.checkbox.onChange { _, isChecked ->
            b.errorText.text = null

            // eggs
            if (isEggs) {
                wantEggs = !isChecked
            }
        }

        if (arguments?.containsKey(credential.keyName) == true) {
            b.checkbox.isChecked = arguments?.getBoolean(credential.keyName) == true
        }

        b.checkbox.tag = credential
        b.checkbox.text = credential.name.resolveString(app)

        return b
    }

    private fun scanQrCode(credential: FormField) {
        val qrDecoderClass = credential.qrDecoderClass ?: return
        app.permissionManager.requestCameraPermission(activity, R.string.permissions_qr_scanner) {
            QrScannerDialog(activity, onCodeScanned = { code ->
                val decoder = qrDecoderClass.getDeclaredConstructor().newInstance()
                val values = decoder.decode(code)
                if (values == null) {
                    Toast.makeText(activity, R.string.login_qr_decoding_error, Toast.LENGTH_SHORT).show()
                    return@QrScannerDialog
                }

                values.forEach { (keyName, fieldText) ->
                    val (_, b) = getCredential<FormField, LoginFormFieldItemBinding>(keyName)
                        ?: return@forEach
                    b.textEdit.setText(fieldText)
                }

                decoder.focusFieldName()?.let { keyName ->
                    val (_, b) = getCredential<FormField, LoginFormFieldItemBinding>(keyName)
                        ?: return@let
                    b.textEdit.requestFocus()
                }
            }).show()
        }
    }

    private fun login(loginType: LoginType, loginMode: LoginMode) {
        val payload = Bundle(
            "loginType" to loginType,
            "loginMode" to loginMode
        )

        if (BuildConfig.DEBUG && b.fakeLogin.isChecked) {
            payload.putBoolean("fakeLogin", true)
        }

        if (platformStoreKey == null)
            payload.putAll(platformData?.toBundle() ?: Bundle())
        else
            payload.putBundle(platformStoreKey, platformData?.toBundle())

        var hasErrors = false
        credentials.forEach { (credential, b) ->
            if (credential is FormField && b is LoginFormFieldItemBinding) {
                var text = b.textEdit.text?.toString() ?: return@forEach
                if (!credential.hideText)
                    text = text.trim()

                if (credential.caseMode == FormField.CaseMode.UPPER_CASE)
                    text = text.uppercase()
                if (credential.caseMode == FormField.CaseMode.LOWER_CASE)
                    text = text.lowercase()

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
            if (credential is FormCheckbox && b is LoginFormCheckboxItemBinding) {
                val checked = b.checkbox.isChecked
                payload.putBoolean(credential.keyName, checked)
                arguments?.putBoolean(credential.keyName, checked)
            }
        }

        if (hasErrors)
            return

        val navOptions =
            if (credentials.isEmpty())
                activity.navOptionsBuilder
                    .setPopUpTo(R.id.loginPlatformListFragment, inclusive = false)
                    .build()
            else
                activity.navOptions

        nav.navigate(R.id.loginProgressFragment, payload, navOptions)
    }
}
