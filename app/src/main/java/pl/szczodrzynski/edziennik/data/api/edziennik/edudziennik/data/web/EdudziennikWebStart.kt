/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_CLASS_DETAIL_ID
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_SCHOOL_DETAIL_ID
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_START
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.get

class EdudziennikWebStart(override val data: DataEdudziennik,
                          val onSuccess: () -> Unit) : EdudziennikWeb(data) {
    companion object {
        private const val TAG = "EdudziennikWebStart"
    }

    init { data.profile?.also { profile ->
        webGet(TAG, data.studentEndpoint + "start") { text ->
            val schoolId = EDUDZIENNIK_SCHOOL_DETAIL_ID.find(text)?.get(1)
            data.schoolId = schoolId

            val classId = EDUDZIENNIK_CLASS_DETAIL_ID.find(text)?.get(1)
            data.classId = classId

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_START, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess() }
}
