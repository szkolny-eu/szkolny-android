/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-23.
 */

package pl.szczodrzynski.edziennik.ui.login.qr

class LoginLibrusQrDecoder : LoginQrDecoder {

    private val regex = "[A-Z0-9_]+".toRegex()

    override fun decode(value: String): Map<String, String>? {
        if (!regex.matches(value) || value.length > 10)
            return null
        return mapOf(
            "accountCode" to value,
        )
    }

    override fun focusFieldName() = "accountPin"
}
