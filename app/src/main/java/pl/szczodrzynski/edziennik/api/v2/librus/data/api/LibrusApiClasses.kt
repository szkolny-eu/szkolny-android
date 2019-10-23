/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-14
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_CLASSES
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiClasses(override val data: DataLibrus,
                       val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiClasses"
    }

    init {
        apiGet(TAG, "Classes") { json ->
            json.getJsonObject("Class")?.also { studentClass ->
                val id = studentClass.getLong("Id") ?: return@also
                val name = studentClass.getString("Number") +
                        studentClass.getString("Symbol")
                val code = data.schoolName + ":" + name
                val teacherId = studentClass.getJsonObject("ClassTutor")?.getLong("Id") ?: -1

                val teamObject = Team(
                        profileId,
                        id,
                        name,
                        1,
                        code,
                        teacherId
                )

                data.teamList.put(id, teamObject)

                data.unitId = studentClass.getJsonObject("Unit").getLong("Id") ?: 0L

                profile?.apply {
                    dateSemester1Start = Date.fromY_m_d(studentClass.getString("BeginSchoolYear")
                            ?: return@apply)
                    dateSemester2Start = Date.fromY_m_d(studentClass.getString("EndFirstSemester")
                            ?: return@apply)
                    dateYearEnd = Date.fromY_m_d(studentClass.getString("EndSchoolYear")
                            ?: return@apply)
                }
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_CLASSES, 4 * DAY)
            onSuccess()
        }
    }
}
