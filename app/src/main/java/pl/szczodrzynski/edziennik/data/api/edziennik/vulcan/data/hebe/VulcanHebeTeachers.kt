/*
 * Copyright (c) Antoni Czaplicki 2021-10-15.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.getString

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
        ) { list, _ ->
            list.forEach { person ->
                val name = person.getString("Name")
                val surname = person.getString("Surname")
                val displayName = person.getString("DisplayName")
                val subjectName = person.getString("Description") ?: return@apiGetList

                val teacher =
                    data.getTeacherByFirstLast(name?.plus(" ")?.plus(surname) ?: displayName ?: return@forEach)

                teacher.addSubject(data.getSubject(null, subjectName).id)
            }
            data.setSyncNext(ENDPOINT_VULCAN_HEBE_TEACHERS, 2 * DAY)
            onSuccess(ENDPOINT_VULCAN_HEBE_TEACHERS)
        }
    }
}
