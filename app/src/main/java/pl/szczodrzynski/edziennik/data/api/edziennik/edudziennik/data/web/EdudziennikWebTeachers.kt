/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-25
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_TEACHERS
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.ext.MONTH
import pl.szczodrzynski.edziennik.ext.get

class EdudziennikWebTeachers(override val data: DataEdudziennik,
                             override val lastSync: Long?,
                             val onSuccess: (endpointId: Int) -> Unit
) : EdudziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "EdudziennikWebTeachers"
    }

    init {
        webGet(TAG, data.studentAndTeacherClassEndpoint + "grid") { text ->
            EDUDZIENNIK_TEACHERS.findAll(text).forEach {
                val lastName = it[1].trim()
                val firstName = it[2].trim()
                data.getTeacher(firstName, lastName)
            }

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_TEACHERS, MONTH)
            onSuccess(ENDPOINT_EDUDZIENNIK_WEB_TEACHERS)
        }
    }
}
