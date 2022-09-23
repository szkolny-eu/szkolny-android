/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import androidx.room.OnConflictStrategy
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MESSAGEBOX_ADDRESSBOOK
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_OTHER
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_PARENT
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_STUDENT
import pl.szczodrzynski.edziennik.data.db.entity.Teacher.Companion.TYPE_TEACHER
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ext.MINUTE
import pl.szczodrzynski.edziennik.ext.getString

class VulcanHebeAddressbook2(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeAddressbook2"
    }

    init { let {
        if (data.messageBoxKey == null) {
            data.setSyncNext(ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2, 30 * MINUTE)
            onSuccess(ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2)
            return@let
        }

        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_MESSAGEBOX_ADDRESSBOOK,
            HebeFilterType.BY_MESSAGEBOX,
            messageBox = data.messageBoxKey,
            lastSync = lastSync,
            includeFilterType = false
        ) { list, _ ->
            list.forEach { person ->
                val teacher = getTeacherRecipient(person) ?: return@forEach
                val group = person.getString("Group", "P")
                if (teacher.type == TYPE_OTHER) {
                    teacher.type = when (group) {
                        "P" -> TYPE_TEACHER // Pracownik
                        "O" -> TYPE_PARENT  // Opiekun
                        "U" -> TYPE_STUDENT // Uczeń
                        else -> TYPE_OTHER
                    }
                }
            }
            data.teacherOnConflictStrategy = OnConflictStrategy.REPLACE
            data.setSyncNext(ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2, 2 * DAY)
            onSuccess(ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2)
        }
    }}
}
