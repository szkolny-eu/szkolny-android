/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-24
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.crc32
import pl.szczodrzynski.edziennik.data.api.ERROR_EDUDZIENNIK_WEB_TEAM_MISSING
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.edziennik.firstLettersName
import pl.szczodrzynski.edziennik.get

class EdudziennikWebStartInfo(val data: DataEdudziennik, val text: String) {
    companion object {
        const val TAG = "EdudziennikWebStartInfo"
    }

    init { run {
        val schoolId = Regexes.EDUDZIENNIK_SCHOOL_DETAIL_ID.find(text)?.get(1)?.trim()
        val schoolLongName = Regexes.EDUDZIENNIK_SCHOOL_DETAIL_NAME.find(text)?.get(1)?.trim()
        data.schoolId = schoolId

        val classId = Regexes.EDUDZIENNIK_CLASS_DETAIL_ID.find(text)?.get(1)?.trim()
        val className = Regexes.EDUDZIENNIK_CLASS_DETAIL_NAME.find(text)?.get(1)?.trim()
        data.classId = classId

        if (classId == null || className == null || schoolId == null || schoolLongName == null) {
            data.error(ApiError(TAG, ERROR_EDUDZIENNIK_WEB_TEAM_MISSING)
                    .withApiResponse(text))
            return@run
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
    }}
}
