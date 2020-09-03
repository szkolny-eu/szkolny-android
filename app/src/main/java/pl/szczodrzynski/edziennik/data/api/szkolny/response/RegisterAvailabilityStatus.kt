/*
 * Copyright (c) Kuba Szczodrzyński 2020-9-2.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.response

import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.currentTimeUnix

data class RegisterAvailabilityStatus(
        val available: Boolean,
        val name: String?,
        val message: Message?,
        val nextCheck: Long = currentTimeUnix() + 7 * DAY,
        val minVersionCode: Int = BuildConfig.VERSION_CODE
) {
    data class Message(
            val title: String,
            val contentShort: String,
            val contentLong: String,
            val icon: String?,
            val image: String?,
            val url: String?
    )
}
