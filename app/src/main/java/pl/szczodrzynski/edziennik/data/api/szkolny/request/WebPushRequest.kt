/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-19.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.request

data class WebPushRequest(
        override val deviceId: String,
        override val device: Device? = null,

        val action: String,

        val browserId: String? = null,
        val pairToken: String? = null
) : ApiRequest(deviceId, device)
