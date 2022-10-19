/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-24.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_NOTICES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiNotices(override val data: DataLibrus,
                       override val lastSync: Long?,
                       val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiNotices"
    }

    init {
        if (data.noticeTypes.isEmpty()) {
            data.db.noticeTypeDao().getAllNow(profileId).toSparseArray(data.noticeTypes) { it.id }
        }

        apiGet(TAG, "Notes") { json ->
            val notes = json.getJsonArray("Notes")?.asJsonObjectList()

            notes?.forEach { note ->
                val id = note.getLong("Id") ?: return@forEach
                val text = note.getString("Text") ?: ""
                val categoryId = note.getJsonObject("Category")?.getLong("Id") ?: -1
                val teacherId = note.getJsonObject("AddedBy")?.getLong("Id") ?: -1
                val addedDate = note.getString("Date")?.let { Date.fromY_m_d(it) } ?: return@forEach

                val type = when (note.getInt("Positive")) {
                    0 -> Notice.TYPE_NEGATIVE
                    1 -> Notice.TYPE_POSITIVE
                    /*2*/else -> Notice.TYPE_NEUTRAL
                }
                val categoryText = data.noticeTypes[categoryId]?.name ?: ""
                val semester = profile?.dateToSemester(addedDate) ?: 1

                val noticeObject = Notice(
                        profileId = profileId,
                        id = id,
                        type = type,
                        semester = semester,
                        text = text,
                        category = categoryText,
                        points = null,
                        teacherId = teacherId,
                        addedDate = addedDate.inMillis
                )

                data.noticeList.add(noticeObject)
                data.metadataList.add(
                        Metadata(
                                profileId,
                                MetadataType.NOTICE,
                                id,
                                profile?.empty ?: false,
                                profile?.empty ?: false
                        ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_NOTICES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_NOTICES)
        }
    }
}
