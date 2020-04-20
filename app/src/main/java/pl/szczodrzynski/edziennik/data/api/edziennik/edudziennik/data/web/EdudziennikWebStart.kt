/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.MONTH
import pl.szczodrzynski.edziennik.crc32
import pl.szczodrzynski.edziennik.data.api.ERROR_EDUDZIENNIK_WEB_TEAM_MISSING
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_SUBJECTS_START
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_START
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Team
import pl.szczodrzynski.edziennik.firstLettersName
import pl.szczodrzynski.edziennik.get

class EdudziennikWebStart(override val data: DataEdudziennik,
                          override val lastSync: Long?,
                          val onSuccess: (endpointId: Int) -> Unit
) : EdudziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "EdudziennikWebStart"
    }

    init {
        webGet(TAG, data.studentEndpoint + "start") { text ->
            getSchoolAndTeam(text)
            getSubjects(text)

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_START, MONTH)
            onSuccess(ENDPOINT_EDUDZIENNIK_WEB_START)
        }
    }

    private fun getSchoolAndTeam(text: String) {
        val schoolId = Regexes.EDUDZIENNIK_SCHOOL_DETAIL_ID.find(text)?.get(1)?.trim()
        val schoolLongName = Regexes.EDUDZIENNIK_SCHOOL_DETAIL_NAME.find(text)?.get(1)?.trim()
        data.schoolId = schoolId

        val classId = Regexes.EDUDZIENNIK_CLASS_DETAIL_ID.find(text)?.get(1)?.trim()
        val className = Regexes.EDUDZIENNIK_CLASS_DETAIL_NAME.find(text)?.get(1)?.trim()
        data.classId = classId

        if (classId == null || className == null || schoolId == null || schoolLongName == null) {
            data.error(ApiError(TAG, ERROR_EDUDZIENNIK_WEB_TEAM_MISSING)
                    .withApiResponse(text))
            return
        }

        val schoolName = schoolId.crc32().toString() + schoolLongName.firstLettersName + "_edu"
        data.schoolName = schoolName

        val teamId = classId.crc32()
        val teamCode = "$schoolName:$className"

        val teamObject = Team(
                data.profileId,
                teamId,
                className,
                Team.TYPE_CLASS,
                teamCode,
                -1
        )

        data.teamClass = teamObject
        data.teamList.put(teamObject.id, teamObject)
    }

    private fun getSubjects(text: String) {
        EDUDZIENNIK_SUBJECTS_START.findAll(text).forEach {
            val id = it[1].trim()
            val name = it[2].trim()
            data.getSubject(id, name)
        }
    }
}
