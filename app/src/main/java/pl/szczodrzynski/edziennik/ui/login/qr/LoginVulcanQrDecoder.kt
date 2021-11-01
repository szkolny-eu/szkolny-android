/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-23.
 */

package pl.szczodrzynski.edziennik.ui.login.qr

import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.utils.Utils

class LoginVulcanQrDecoder : LoginQrDecoder {

    private val regex = "CERT#https?://.+?/([A-z]+)/mobile-api#([A-z0-9]+)#ENDCERT".toRegex()

    override fun decode(value: String): Map<String, String>? {
        val data = try {
            Utils.VulcanQrEncryptionUtils.decode(value)
        } catch (e: Exception) {
            return null
        }

        val match = regex.find(data) ?: return null
        return mapOf(
            "deviceToken" to match[2],
            "symbol" to match[1],
        )
    }

    override fun focusFieldName() = "devicePin"
}
