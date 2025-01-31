/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-15.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_API_INCOMPLETE_RESPONSE
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.ENDPOINT_USOS_API_COURSES
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory
import pl.szczodrzynski.edziennik.data.db.entity.Team
import pl.szczodrzynski.edziennik.ext.*

class UsosApiCourses(
    override val data: DataUsos,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : UsosApi(data, lastSync) {
    companion object {
        const val TAG = "UsosApiCourses"
    }

    init {
        apiRequest<JsonObject>(
            tag = TAG,
            service = "courses/user",
            params = mapOf(
                "active_terms_only" to false,
            ),
            fields = listOf(
                // "terms" to listOf("id", "name", "start_date", "end_date"),
                "course_editions" to listOf(
                    "course_id",
                    "course_name",
                    "user_groups" to listOf(
                        "course_unit_id",
                        "group_number",
                        "class_type",
                        "class_type_id",
                        "term_id",
                        "lecturers",
                    ),
                ),
            ),
            responseType = ResponseType.OBJECT,
        ) { json, response ->
            if (!processResponse(json)) {
                data.error(TAG, ERROR_USOS_API_INCOMPLETE_RESPONSE, response)
                return@apiRequest
            }

            data.setSyncNext(ENDPOINT_USOS_API_COURSES, 2 * DAY)
            onSuccess(ENDPOINT_USOS_API_COURSES)
        }
    }

    private fun processResponse(json: JsonObject): Boolean {
        // val term = json.getJsonArray("terms")?.firstOrNull() ?: return false
        val courseEditions = json.getJsonObject("course_editions")
            ?.entrySet()
            ?.flatMap { it.value.asJsonArray }
            ?.map { it.asJsonObject } ?: return false

        var hasValidTeam = false
        for (courseEdition in courseEditions) {
            val courseId = courseEdition.getString("course_id") ?: continue
            val courseName = courseEdition.getLangString("course_name") ?: continue
            val userGroups =
                courseEdition.getJsonArray("user_groups")?.asJsonObjectList() ?: continue
            for (userGroup in userGroups) {
                val courseUnitId = userGroup.getLong("course_unit_id") ?: continue
                val groupNumber = userGroup.getInt("group_number") ?: continue
                val classType = userGroup.getLangString("class_type") ?: continue
                val classTypeId = userGroup.getString("class_type_id") ?: continue
                val termId = userGroup.getString("term_id") ?: continue
                val lecturers = userGroup.getLecturerIds("lecturers")

                data.teamList.put(
                    courseUnitId, Team(
                        profileId,
                        courseUnitId,
                        "${profile?.studentClassName} $courseName ($classTypeId$groupNumber)",
                        2,
                        "${data.schoolId}:${termId}:${courseId} $classTypeId$groupNumber",
                        lecturers.firstOrNull() ?: -1L,
                    )
                )

                val gradeCategory = data.gradeCategories[courseUnitId]
                data.gradeCategories.put(
                    courseUnitId, GradeCategory(
                        profileId,
                        courseUnitId,
                        gradeCategory?.weight ?: -1.0f,
                        0,
                        courseId,
                    ).addColumn(classType)
                )

                hasValidTeam = true
            }
        }
        return hasValidTeam
    }
}
