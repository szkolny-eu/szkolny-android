/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-7.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.api

import android.graphics.Color
import androidx.core.util.contains
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory

class MobidziennikApiGradeCategories(val data: DataMobidziennik, rows: List<String>) {
    init {
        for (row in rows) {
            if (row.isEmpty())
                continue
            val cols = row.split("|")

            val teamId = cols[1].toLong()
            if (data.teamList.contains(teamId)) {

                val id = cols[0].toLong()
                val weight = cols[3].toFloat()
                val color = Color.parseColor("#" + cols[6])
                val category = cols[4]
                val columns = cols[7].split(";")

                data.gradeCategories.put(
                        id,
                        GradeCategory(
                                data.profileId,
                                id,
                                weight,
                                color,
                                category
                        ).addColumns(columns)
                )
            }
        }
    }
}