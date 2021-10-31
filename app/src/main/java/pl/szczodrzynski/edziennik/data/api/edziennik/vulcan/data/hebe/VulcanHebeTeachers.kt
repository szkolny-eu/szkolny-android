/*
 * Copyright (c) Antoni Czaplicki 2021-10-15.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import androidx.room.OnConflictStrategy
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ext.getString

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
                val subjectName = person.getString("Description") ?: return@forEach

                if (subjectName.isBlank()) {
                    return@forEach
                }

                val teacher = data.getTeacherByFirstLast(
                    name?.plus(" ")?.plus(surname) ?: displayName ?: return@forEach
                )

                when (subjectName) {
                    "Pedagog" -> teacher.setTeacherType(Teacher.TYPE_PEDAGOGUE)
                    else -> {
                        val subjectId = data.getSubject(null, subjectName).id
                        if (!teacher.subjects.contains(subjectId))
                            teacher.addSubject(subjectId)
                    }
                }
            }
            data.teacherOnConflictStrategy = OnConflictStrategy.REPLACE
            data.setSyncNext(ENDPOINT_VULCAN_HEBE_TEACHERS, 2 * DAY)
            onSuccess(ENDPOINT_VULCAN_HEBE_TEACHERS)
        }
    }
}
