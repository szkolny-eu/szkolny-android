/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-4.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_SCHOOLS
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.LessonRange
import pl.szczodrzynski.edziennik.utils.models.Time
import java.util.*

class LibrusApiSchools(override val data: DataLibrus,
                       override val lastSync: Long?,
                       val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiSchools"
    }

    init {
        apiGet(TAG, "Schools") { json ->
            val school = json.getJsonObject("School")
            val schoolId = school?.getInt("Id")
            val schoolNameLong = school?.getString("Name")

            // create the school's short name using first letters of each long name's word
            // append the town name and save to student data
            val schoolNameShort = schoolNameLong?.firstLettersName
            val schoolTown = school?.getString("Town")?.toLowerCase(Locale.getDefault())
            data.schoolName = schoolId.toString() + schoolNameShort + "_" + schoolTown

            school?.getJsonArray("LessonsRange")?.let { ranges ->
                data.lessonRanges.clear()
                ranges.forEachIndexed { index, rangeEl ->
                    val range = rangeEl.asJsonObject
                    val from = range.getString("From") ?: return@forEachIndexed
                    val to = range.getString("To") ?: return@forEachIndexed
                    data.lessonRanges.put(
                            index,
                            LessonRange(
                                    profileId,
                                    index,
                                    Time.fromH_m(from),
                                    Time.fromH_m(to)
                            ))
                }
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_SCHOOLS, 4 * DAY)
            onSuccess(ENDPOINT_LIBRUS_API_SCHOOLS)
        }
    }
}
