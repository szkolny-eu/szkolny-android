/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-20
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.api.v2.VULCAN_API_ENDPOINT_DICTIONARIES
import pl.szczodrzynski.edziennik.api.v2.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.api.v2.vulcan.ENDPOINT_VULCAN_API_DICTIONARIES
import pl.szczodrzynski.edziennik.api.v2.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString

class VulcanApiDictionaries(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApiDictionaries"
    }

    init {
        apiGet(TAG, VULCAN_API_ENDPOINT_DICTIONARIES) { json, _ ->
            val elements = json.getJsonObject("Data")

            elements?.getJsonArray("Pracownicy")?.forEach { saveTeacher(it.asJsonObject) }
            elements?.getJsonArray("Przedmioty")?.forEach { saveSubject(it.asJsonObject) }

            data.setSyncNext(ENDPOINT_VULCAN_API_DICTIONARIES, SYNC_ALWAYS)
            onSuccess()
        }
    }

    private fun saveTeacher(teacher: JsonObject) {
        val id = teacher.getLong("Id") ?: return
        val name = teacher.getString("Imie") ?: ""
        val surname = teacher.getString("Nazwisko") ?: ""
        val loginId = teacher.getString("LoginId") ?: "-1"

        val teacherObject = Teacher(
                profileId,
                id,
                name,
                surname,
                loginId
        )

        data.teacherList.put(id, teacherObject)
    }

    private fun saveSubject(subject: JsonObject) {
        val id = subject.getLong("Id") ?: return
        val longName = subject.getString("Nazwa") ?: ""
        val shortName = subject.getString("Kod") ?: ""

        val subjectObject = Subject(
                profileId,
                id,
                longName,
                shortName
        )

        data.subjectList.put(id, subjectObject)
    }
}
