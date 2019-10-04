/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-4.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import android.util.Pair
import com.google.gson.JsonNull
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Time
import java.util.*

class LibrusApiSchools(override val data: DataLibrus,
                        val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiSchools"
    }

    init {
        apiGet(LibrusApiMe.TAG, "") { json ->
            val school = json?.getJsonObject("School")
            val schoolId = school?.getInt("Id")
            val schoolNameLong = school?.getString("Name")

            var schoolNameShort = ""
            schoolNameLong?.split(" ")?.forEach {
                if (it.isBlank())
                    return@forEach
                schoolNameShort += it[0].toLowerCase()
            }
            val schoolTown = school?.getString("Town")?.toLowerCase(Locale.getDefault())
            data.schoolName = schoolId.toString() + schoolNameShort + "_" + schoolTown

            /*lessonRanges.clear()
            for ((index, lessonRangeEl) in school.get("LessonsRange").getAsJsonArray().withIndex()) {
                val lr = lessonRangeEl.getAsJsonObject()
                val from = lr.get("From")
                val to = lr.get("To")
                if (from != null && to != null && from !is JsonNull && to !is JsonNull) {
                    lessonRanges.put(index, Pair<Time, Time>(Time.fromH_m(from!!.getAsString()), Time.fromH_m(to!!.getAsString())))
                }
            }
            profile.putStudentData("lessonRanges", app.gson.toJson(lessonRanges))
            // on error
            data.error(TAG, ERROR_LIBRUS_API_, response, json)

            data.setSyncNext(ENDPOINT_LIBRUS_API_, 2 * DAY)
            onSuccess()*/
        }
    }
}