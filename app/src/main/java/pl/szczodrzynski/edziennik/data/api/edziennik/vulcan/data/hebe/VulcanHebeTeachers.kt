/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.singleOrNull

class VulcanHebeTeachers(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeTeachers"
    }

    init {
        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_TEACHERS,
            HebeFilterType.BY_PERIOD,
            lastSync = lastSync,
            includeFilterType = false
        ) { list, _ ->
            list.forEach { person ->
                val name = person.getString("Name") ?: ""
                val surname = person.getString("Surname") ?: ""
                val displayName = person.getString("DisplayName") ?: ""
                val subject = person.getString("Description")

                val teacher =
                    data.teacherList.singleOrNull { (it.name == name && it.surname == surname) || it.fullName == displayName }
                if (subject != null) {
                    teacher?.addTeacherSubject(subject)
                }
            }
            data.setSyncNext(ENDPOINT_VULCAN_HEBE_TEACHERS, 2 * DAY)
            onSuccess(ENDPOINT_VULCAN_HEBE_TEACHERS)
        }
    }
}
