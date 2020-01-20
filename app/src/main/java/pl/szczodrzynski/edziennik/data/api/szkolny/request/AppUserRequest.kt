/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-18.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.request

data class AppUserRequest(
        override val deviceId: String,
        override val device: Device? = null,

        val action: String = "unregister",
        val userCode: String
) : ApiRequest(deviceId, device)
