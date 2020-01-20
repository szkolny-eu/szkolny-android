/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-20.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.request

open class ApiRequest(
        open val deviceId: String,
        open val device: Device? = null
) {
    data class Device(
            val osType: String,
            val osVersion: String,
            val hardware: String,
            val pushToken: String?,
            val appVersion: String,
            val appType: String,
            val appVersionCode: Int,
            val syncInterval: Int
    )
}
