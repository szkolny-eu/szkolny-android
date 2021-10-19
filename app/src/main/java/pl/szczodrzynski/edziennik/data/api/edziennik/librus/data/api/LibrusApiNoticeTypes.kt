/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-24.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_NOTICE_TYPES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.NoticeType
import pl.szczodrzynski.edziennik.ext.*

class LibrusApiNoticeTypes(override val data: DataLibrus,
                           override val lastSync: Long?,
                           val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiNoticeTypes"
    }

    init {
        apiGet(TAG, "Notes/Categories") { json ->
            val noticeTypes = json.getJsonArray("Categories")?.asJsonObjectList()

            noticeTypes?.forEach { noticeType ->
                val id = noticeType.getLong("Id") ?: return@forEach
                val name = noticeType.getString("CategoryName") ?: ""

                data.noticeTypes.put(id, NoticeType(profileId, id, name))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_NOTICE_TYPES, 4* DAY)
            onSuccess(ENDPOINT_LIBRUS_API_NOTICE_TYPES)
        }
    }
}
