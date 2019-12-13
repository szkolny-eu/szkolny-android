/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.mobidziennik.data.api

import pl.szczodrzynski.edziennik.api.v2.ERROR_MOBIDZIENNIK_WEB_INVALID_RESPONSE
import pl.szczodrzynski.edziennik.api.v2.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.api.v2.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_API_MAIN
import pl.szczodrzynski.edziennik.api.v2.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS

class MobidziennikApi(override val data: DataMobidziennik,
                      val onSuccess: () -> Unit) : MobidziennikWeb(data)  {
    companion object {
        private const val TAG = "MobidziennikApi"
    }

    init {
        webGet(TAG, "/api/zrzutbazy") { text ->
            if (!text.contains("T@B#LA")) {
                data.error(ApiError(TAG, ERROR_MOBIDZIENNIK_WEB_INVALID_RESPONSE)
                        .withApiResponse(text))
                return@webGet
            }

            val tables = text.split("T@B#LA")
            tables.forEachIndexed { index, table ->
                val rows = table.split("\n")
                when (index) {
                    0 -> MobidziennikApiUsers(data, rows)
                    3 -> MobidziennikApiDates(data, rows)
                    4 -> MobidziennikApiSubjects(data, rows)
                    7 -> MobidziennikApiTeams(data, rows, null)
                    8 -> MobidziennikApiStudent(data, rows)
                    9 -> MobidziennikApiTeams(data, null, rows)
                    14 -> MobidziennikApiGradeCategories(data, rows)
                    15 -> MobidziennikApiLessons(data, rows)
                    16 -> MobidziennikApiAttendance(data, rows)
                    17 -> MobidziennikApiNotices(data, rows)
                    18 -> MobidziennikApiGrades(data, rows)
                    21 -> MobidziennikApiEvents(data, rows)
                    23 -> MobidziennikApiHomework(data, rows)
                    24 -> MobidziennikApiTimetable(data, rows)
                }
            }

            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_API_MAIN, SYNC_ALWAYS)
            onSuccess()
        }
    }
}
