/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-19.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.request

data class WebPushRequest(

        val action: String,
        val deviceId: String,

        val browserId: String? = null,
        val pairToken: String? = null
)