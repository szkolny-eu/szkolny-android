/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-13
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ext.getString

class PodlasieApiTeachers(val data: DataPodlasie, val rows: List<JsonObject>) {
    init {
        rows.forEach { teacher ->
            val id = teacher.getLong("ExternalId") ?: return@forEach
            val firstName = teacher.getString("FirstName") ?: return@forEach
            val lastName = teacher.getString("LastName") ?: return@forEach
            val isEducator = teacher.getInt("Educator") == 1

            val teacherObject = Teacher(
                    profileId = data.profileId,
                    id = id,
                    name = firstName,
                    surname = lastName,
                    loginId = null
            )

            data.teacherList.put(id, teacherObject)

            val teamClass = data.teamClass
            if (isEducator && teamClass != null) {
                data.teamList.put(teamClass.id, teamClass.apply {
                    teacherId = id
                })
            }
        }
    }
}
