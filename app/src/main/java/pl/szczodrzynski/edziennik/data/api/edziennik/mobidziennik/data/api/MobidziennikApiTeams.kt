/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.entity.Team
import pl.szczodrzynski.edziennik.getById
import pl.szczodrzynski.edziennik.values

class MobidziennikApiTeams(val data: DataMobidziennik, tableTeams: List<String>?, tableRelations: List<String>?) {
    init {
        if (tableTeams != null) {
            for (row in tableTeams) {
                if (row.isEmpty())
                    continue
                val cols = row.split("|")

                val id = cols[0].toLong()
                val name = cols[1]+cols[2]
                val type = cols[3].toInt()
                val code = data.loginServerName+":"+name
                val teacherId = cols[4].toLongOrNull() ?: -1

                val teamObject = Team(
                        data.profileId,
                        id,
                        name,
                        type,
                        code,
                        teacherId)
                data.teamList.put(id, teamObject)
            }
        }
        if (tableRelations != null) {
            val allTeams = data.teamList.values()
            data.teamList.clear()

            for (row in tableRelations) {
                if (row.isEmpty())
                    continue
                val cols = row.split("|")

                val studentId = cols[1].toInt()
                val teamId = cols[2].toLong()
                val studentNumber = cols[4].toInt()

                if (studentId != data.studentId)
                    continue
                val team = allTeams.getById(teamId)
                if (team != null) {
                    if (team.type == 1) {
                        data.profile?.studentNumber = studentNumber
                        data.teamClass = team
                        data.profile?.studentClassName = team.name
                    }
                    data.teamList.put(teamId, team)
                }
            }
        }
    }
}
