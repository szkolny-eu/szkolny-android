/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-16.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_NO_STUDENT_PROGRAMMES
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.ENDPOINT_USOS_API_USER
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ext.*

class UsosApiUser(
    override val data: DataUsos,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : UsosApi(data, lastSync) {
    companion object {
        const val TAG = "UsosApiUser"
    }

    init {
        apiRequest<JsonObject>(
            tag = TAG,
            service = "users/user",
            params = mapOf(
                "fields" to listOf(
                    "id",
                    "first_name",
                    "last_name",
                    "student_number",
                    "student_programmes" to listOf(
                        "id",
                        "programme" to listOf("id", "description"),
                    ),
                ),
            ),
            responseType = ResponseType.OBJECT,
        ) { json, response ->
            val programmes = json.getJsonArray("student_programmes")
            if (programmes.isNullOrEmpty()) {
                data.error(
                    ApiError(TAG, ERROR_USOS_NO_STUDENT_PROGRAMMES)
                        .withApiResponse(json)
                        .withResponse(response)
                )
                return@apiRequest
            }

            val firstName = json.getString("first_name")
            val lastName = json.getString("last_name")
            val studentName = buildFullName(firstName, lastName)

            val studentProgrammeId = programmes.getJsonObject(0)
                .getString("id")
            val programmeId = programmes.getJsonObject(0)
                .getJsonObject("programme")
                .getString("id")

            data.studentId = json.getInt("id") ?: data.studentId
            profile?.studentNameLong = studentName
            profile?.studentNameShort = studentName.getShortName()
            profile?.studentNumber = json.getString("student_number")?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: -1
            profile?.studentClassName = programmeId

            val team = programmeId?.let {
                data.getTeam(
                    id = null,
                    name = it,
                    schoolCode = data.schoolId ?: "",
                    isTeamClass = true,
                )
            }
            team?.code = "${data.schoolId}:${studentProgrammeId}:${programmeId}"

            data.setSyncNext(ENDPOINT_USOS_API_USER, 4 * DAY)
            onSuccess(ENDPOINT_USOS_API_USER)
        }
    }
}
