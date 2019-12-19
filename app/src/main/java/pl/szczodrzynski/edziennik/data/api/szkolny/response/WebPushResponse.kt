/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-19.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.response

data class WebPushResponse(val browsers: List<Browser>) {
    data class Browser(
            val id: Int,
            val browserId: String,
            val userAgent: String,
            val dateRegistered: String
    )
}