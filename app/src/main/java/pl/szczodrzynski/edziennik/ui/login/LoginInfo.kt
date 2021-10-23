/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.login

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.ui.grades.models.ExpandableItemModel
import pl.szczodrzynski.fslogin.realm.RealmData

object LoginInfo {

    private fun getEmailCredential(keyName: String) = FormField(
        keyName = keyName,
        name = R.string.login_hint_email,
        icon = CommunityMaterial.Icon.cmd_at,
        emptyText = R.string.login_error_no_email,
        invalidText = R.string.login_error_incorrect_email,
        errorCodes = mapOf(),
        isRequired = true,
        validationRegex = "([\\w.\\-_+]+)?\\w+@[\\w-_]+(\\.\\w+)+",
        caseMode = FormField.CaseMode.LOWER_CASE
    )

    private fun getPasswordCredential(keyName: String) = FormField(
        keyName = keyName,
        name = R.string.login_hint_password,
        icon = CommunityMaterial.Icon2.cmd_lock_outline,
        emptyText = R.string.login_error_no_password,
        invalidText = R.string.login_error_incorrect_login_or_password,
        errorCodes = mapOf(),
        isRequired = true,
        validationRegex = ".*",
        hideText = true
    )

    val list by lazy {
        listOf(
            Register(
                loginType = LOGIN_TYPE_LIBRUS,
                internalName = "librus",
                registerName = R.string.login_register_librus,
                registerLogo = R.drawable.login_logo_librus,
                loginModes = listOf(
                    Mode(
                        loginMode = LOGIN_MODE_LIBRUS_EMAIL,
                        name = R.string.login_mode_librus_email,
                        icon = R.drawable.login_mode_librus_email,
                        hintText = R.string.login_mode_librus_email_hint,
                        guideText = R.string.login_mode_librus_email_guide,
                        isRecommended = true,
                        credentials = listOf(
                            getEmailCredential("email"),
                            getPasswordCredential("password")
                        ),
                        errorCodes = mapOf(
                            ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED to R.string.login_error_account_not_activated,
                            ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN to R.string.login_error_incorrect_login_or_password,
                            ERROR_CAPTCHA_LIBRUS_PORTAL to R.string.error_3001_reason
                        )
                    ),
                    /*Mode(
                            loginMode = LOGIN_MODE_LIBRUS_SYNERGIA,
                            name = R.string.login_mode_librus_synergia,
                            icon = R.drawable.login_mode_librus_synergia,
                            hintText = R.string.login_mode_librus_synergia_hint,
                            guideText = R.string.login_mode_librus_synergia_guide,
                            credentials = listOf(
                                    Credential(
                                            keyName = "accountLogin",
                                            name = R.string.login_hint_login,
                                            icon = CommunityMaterial.Icon.cmd_account_outline,
                                            emptyText = R.string.login_error_no_login,
                                            invalidText = R.string.login_error_incorrect_login,
                                            errorCodes = mapOf(),
                                            isRequired = true,
                                            validationRegex = "[A-z0-9._\\-+]+",
                                            caseMode = Credential.CaseMode.LOWER_CASE
                                    ),
                                    getPasswordCredential("accountPassword")
                            ),
                            errorCodes = mapOf(
                                    ERROR_LOGIN_LIBRUS_API_INVALID_LOGIN to R.string.login_error_incorrect_login_or_password,
                                    ERROR_LOGIN_LIBRUS_API_INVALID_REQUEST to R.string.login_error_incorrect_login_or_password
                            )
                    ),*/
                    Mode(
                        loginMode = LOGIN_MODE_LIBRUS_JST,
                        name = R.string.login_mode_librus_jst,
                        icon = R.drawable.login_mode_librus_jst,
                        hintText = R.string.login_mode_librus_jst_hint,
                        guideText = R.string.login_mode_librus_jst_guide,
                        credentials = listOf(
                            FormField(
                                keyName = "accountCode",
                                name = R.string.login_hint_token,
                                icon = CommunityMaterial.Icon.cmd_code_braces,
                                emptyText = R.string.login_error_no_token,
                                invalidText = R.string.login_error_incorrect_token,
                                errorCodes = mapOf(),
                                isRequired = true,
                                validationRegex = "[A-Z0-9_]+",
                                caseMode = FormField.CaseMode.UPPER_CASE
                            ),
                            FormField(
                                keyName = "accountPin",
                                name = R.string.login_hint_pin,
                                icon = CommunityMaterial.Icon2.cmd_lock_outline,
                                emptyText = R.string.login_error_no_pin,
                                invalidText = R.string.login_error_incorrect_pin,
                                errorCodes = mapOf(),
                                isRequired = true,
                                validationRegex = "[a-z0-9_]+",
                                caseMode = FormField.CaseMode.LOWER_CASE
                            )
                        ),
                        errorCodes = mapOf(
                            ERROR_LOGIN_LIBRUS_API_INVALID_LOGIN to R.string.login_error_incorrect_code_or_pin,
                            ERROR_LOGIN_LIBRUS_API_INVALID_REQUEST to R.string.login_error_incorrect_code_or_pin
                        )
                    )
                )
            ),
            Register(
                loginType = LOGIN_TYPE_VULCAN,
                internalName = "vulcan",
                registerName = R.string.login_type_vulcan,
                registerLogo = R.drawable.login_logo_vulcan,
                loginModes = listOf(
                    Mode(
                        loginMode = LOGIN_MODE_VULCAN_HEBE,
                        name = R.string.login_mode_vulcan_api,
                        icon = R.drawable.login_mode_vulcan_hebe,
                        hintText = R.string.login_mode_vulcan_api_hint,
                        guideText = R.string.login_mode_vulcan_api_guide,
                        isRecommended = true,
                        credentials = listOf(
                            FormField(
                                keyName = "deviceToken",
                                name = R.string.login_hint_token,
                                icon = CommunityMaterial.Icon.cmd_code_braces,
                                emptyText = R.string.login_error_no_token,
                                invalidText = R.string.login_error_incorrect_token,
                                errorCodes = mapOf(
                                    ERROR_LOGIN_VULCAN_INVALID_TOKEN to R.string.login_error_incorrect_token,
                                    ERROR_LOGIN_VULCAN_EXPIRED_TOKEN to R.string.login_error_expired_token
                                ),
                                isRequired = true,
                                validationRegex = "[A-Z0-9]{5,12}",
                                caseMode = FormField.CaseMode.UPPER_CASE
                            ),
                            FormField(
                                keyName = "symbol",
                                name = R.string.login_hint_symbol,
                                icon = CommunityMaterial.Icon3.cmd_school_outline,
                                emptyText = R.string.login_error_no_symbol,
                                invalidText = R.string.login_error_incorrect_symbol,
                                errorCodes = mapOf(
                                    ERROR_LOGIN_VULCAN_INVALID_SYMBOL to R.string.login_error_incorrect_symbol
                                ),
                                isRequired = true,
                                validationRegex = "[a-z0-9_-]+",
                                caseMode = FormField.CaseMode.LOWER_CASE
                            ),
                            FormField(
                                keyName = "devicePin",
                                name = R.string.login_hint_pin,
                                icon = CommunityMaterial.Icon2.cmd_lock_outline,
                                emptyText = R.string.login_error_no_pin,
                                invalidText = R.string.login_error_incorrect_pin,
                                errorCodes = mapOf(
                                    ERROR_LOGIN_VULCAN_INVALID_PIN_0_REMAINING to R.string.error_310_reason,
                                    ERROR_LOGIN_VULCAN_INVALID_PIN_1_REMAINING to R.string.error_311_reason,
                                    ERROR_LOGIN_VULCAN_INVALID_PIN_2_REMAINING to R.string.error_312_reason
                                ),
                                isRequired = true,
                                isNumber = true,
                                validationRegex = "[0-9]+",
                                caseMode = FormField.CaseMode.LOWER_CASE
                            )
                        ),
                        errorCodes = mapOf()
                    ),
                    Mode(
                        loginMode = LOGIN_MODE_VULCAN_WEB,
                        name = R.string.login_mode_vulcan_web,
                        icon = R.drawable.login_mode_vulcan_web,
                        hintText = R.string.login_mode_vulcan_web_hint,
                        guideText = R.string.login_mode_vulcan_web_guide,
                        isTesting = true,
                        isPlatformSelection = true,
                        credentials = listOf(
                            getEmailCredential("email"),
                            FormField(
                                keyName = "username",
                                name = R.string.login_hint_username,
                                icon = CommunityMaterial.Icon.cmd_account_outline,
                                emptyText = R.string.login_error_no_username,
                                invalidText = R.string.login_error_incorrect_username,
                                errorCodes = mapOf(),
                                isRequired = true,
                                validationRegex = "[A-Z]{7}[0-9]+",
                                caseMode = FormField.CaseMode.UPPER_CASE
                            ),
                            getPasswordCredential("password")
                        ),
                        errorCodes = mapOf()
                    )
                )
            ),
            Register(
                loginType = LOGIN_TYPE_MOBIDZIENNIK,
                internalName = "mobidziennik",
                registerName = R.string.login_type_mobidziennik,
                registerLogo = R.drawable.login_logo_mobidziennik,
                loginModes = listOf(
                    Mode(
                        loginMode = LOGIN_MODE_MOBIDZIENNIK_WEB,
                        name = R.string.login_mode_mobidziennik_web,
                        icon = R.drawable.login_mode_mobidziennik_web,
                        hintText = R.string.login_mode_mobidziennik_web_hint,
                        guideText = R.string.login_mode_mobidziennik_web_guide,
                        credentials = listOf(
                            FormField(
                                keyName = "username",
                                name = R.string.login_hint_login_email,
                                icon = CommunityMaterial.Icon.cmd_account_outline,
                                emptyText = R.string.login_error_no_login,
                                invalidText = R.string.login_error_incorrect_login,
                                errorCodes = mapOf(),
                                isRequired = true,
                                validationRegex = "^[a-z0-9_\\-@+.]+$",
                                caseMode = FormField.CaseMode.LOWER_CASE
                            ),
                            FormField(
                                keyName = "password",
                                name = R.string.login_hint_password,
                                icon = CommunityMaterial.Icon2.cmd_lock_outline,
                                emptyText = R.string.login_error_no_password,
                                invalidText = R.string.login_error_incorrect_login_or_password,
                                errorCodes = mapOf(
                                    ERROR_LOGIN_MOBIDZIENNIK_WEB_OLD_PASSWORD to R.string.login_error_old_password
                                ),
                                isRequired = true,
                                validationRegex = ".*",
                                hideText = true
                            ),
                            FormField(
                                keyName = "serverName",
                                name = R.string.login_hint_address,
                                icon = CommunityMaterial.Icon3.cmd_web,
                                emptyText = R.string.login_error_no_address,
                                invalidText = R.string.login_error_incorrect_address,
                                errorCodes = mapOf(
                                    ERROR_LOGIN_MOBIDZIENNIK_WEB_INVALID_ADDRESS to R.string.login_error_incorrect_address
                                ),
                                prefix = R.string.login_mobidziennik_server_prefix,
                                suffix = R.string.login_mobidziennik_server_suffix,
                                isRequired = true,
                                validationRegex = "^[a-z0-9_\\-]+\$",
                                caseMode = FormField.CaseMode.LOWER_CASE
                            )
                        ),
                        errorCodes = mapOf(
                            ERROR_LOGIN_MOBIDZIENNIK_WEB_INVALID_LOGIN to R.string.login_error_incorrect_login_or_password,
                            ERROR_LOGIN_MOBIDZIENNIK_WEB_ARCHIVED to R.string.sync_error_archived
                        )
                    )
                )
            ),
            Register(
                loginType = LOGIN_TYPE_EDUDZIENNIK,
                internalName = "edudziennik",
                registerName = R.string.login_type_edudziennik,
                registerLogo = R.drawable.login_logo_edudziennik,
                loginModes = listOf(
                    Mode(
                        loginMode = LOGIN_MODE_EDUDZIENNIK_WEB,
                        name = R.string.login_mode_edudziennik_web,
                        icon = R.drawable.login_mode_edudziennik_web,
                        hintText = R.string.login_mode_edudziennik_web_hint,
                        guideText = R.string.login_mode_edudziennik_web_guide,
                        credentials = listOf(
                            getEmailCredential("email"),
                            getPasswordCredential("password")
                        ),
                        errorCodes = mapOf(
                            ERROR_LOGIN_EDUDZIENNIK_WEB_INVALID_LOGIN to R.string.login_error_incorrect_login_or_password
                        )
                    )
                )
            ),
            Register(
                loginType = LOGIN_TYPE_PODLASIE,
                internalName = "podlasie",
                registerName = R.string.login_type_podlasie,
                registerLogo = R.drawable.login_logo_podlasie,
                loginModes = listOf(
                    Mode(
                        loginMode = LOGIN_MODE_PODLASIE_API,
                        name = R.string.login_mode_podlasie_api,
                        icon = R.drawable.login_mode_podlasie_api,
                        guideText = R.string.login_mode_podlasie_api_guide,
                        credentials = listOf(
                            FormField(
                                keyName = "apiToken",
                                name = R.string.login_hint_token,
                                icon = CommunityMaterial.Icon2.cmd_lock_outline,
                                emptyText = R.string.login_error_no_token,
                                invalidText = R.string.login_error_incorrect_token,
                                errorCodes = mapOf(),
                                isRequired = true,
                                validationRegex = "[a-zA-Z0-9]{10}",
                                caseMode = FormField.CaseMode.UNCHANGED
                            ),
                            FormCheckbox(
                                keyName = "logoutDevices",
                                name = R.string.login_podlasie_logout_devices,
                                checked = false,
                                errorCodes = mapOf(
                                    ERROR_LOGIN_PODLASIE_API_DEVICE_LIMIT to R.string.error_602_reason
                                )
                            )
                        ),
                        errorCodes = mapOf()
                    )
                )
            )
        )
    }

    data class Register(
        val loginType: Int,
        val internalName: String,
        val registerName: Int,
        @DrawableRes
        val registerLogo: Int,

        val loginModes: List<Mode>
    ) : ExpandableItemModel<Mode>(loginModes.toMutableList()) {
        override var level = 1
    }

    data class Mode(
        val loginMode: Int,

        @StringRes
        val name: Int,
        @DrawableRes
        val icon: Int,
        @StringRes
        val hintText: Int? = null,
        @StringRes
        val guideText: Int,

        val isRecommended: Boolean = false,
        val isTesting: Boolean = false,
        val isDevOnly: Boolean = false,
        val isPlatformSelection: Boolean = false,

        val credentials: List<BaseCredential>,
        val errorCodes: Map<Int, Int>
    )

    data class Platform(
        val id: Int,
        val name: String,
        val description: String?,
        val icon: String,
        val screenshot: String?,
        val formFields: List<String>,
        val realmData: RealmData
    )

    open class BaseCredential(
        open val keyName: String,
        @StringRes
        open val name: Int,
        open val errorCodes: Map<Int, Int>
    )

    data class FormField(
        override val keyName: String,

        @StringRes
        override val name: Int,
        val icon: IIcon,
        @StringRes
        val placeholder: Int? = null,
        @StringRes
        val emptyText: Int,
        @StringRes
        val invalidText: Int,
        override val errorCodes: Map<Int, Int>,
        @StringRes
        val hintText: Int? = null,
        @StringRes
        val prefix: Int? = null,
        @StringRes
        val suffix: Int? = null,

        val isRequired: Boolean = true,
        val validationRegex: String,
        val caseMode: CaseMode = CaseMode.UNCHANGED,
        val hideText: Boolean = false,
        val isNumber: Boolean = false,
        val stripTextRegex: String? = null,
        val qrDecoderClass: String? = null
    ) : BaseCredential(keyName, name, errorCodes) {
        enum class CaseMode { UNCHANGED, UPPER_CASE, LOWER_CASE }
    }

    data class FormCheckbox(
        override val keyName: String,
        @StringRes
        override val name: Int,
        val checked: Boolean = false,
        override val errorCodes: Map<Int, Int> = mapOf()
    ) : BaseCredential(keyName, name, errorCodes)

    var chooserList: MutableList<Any>? = null
    var platformList: MutableMap<Int, List<Platform>> = mutableMapOf()
}
