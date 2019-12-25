/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-25
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.MONTH
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.get

class EdudziennikWebTeachers(override val data: DataEdudziennik,
                             val onSuccess: () -> Unit) : EdudziennikWeb(data) {
    companion object {
        private const val TAG = "EdudziennikWebTeachers"
    }

    init { data.profile?.also { profile ->
        webGet(TAG, data.studentAndTeacherClassEndpoint + "grid") { text ->
            EDUDZIENNIK_TEACHERS.findAll(text).forEach {
                val lastName = it[1].trim()
                val firstName = it[2].trim()
                data.getTeacher(firstName, lastName)
            }

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_TEACHERS, MONTH)
            onSuccess()
        }
    } ?: onSuccess() }
}
