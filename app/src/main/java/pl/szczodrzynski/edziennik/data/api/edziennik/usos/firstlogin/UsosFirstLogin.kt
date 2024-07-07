/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-14.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.firstlogin

import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_NO_STUDENT_PROGRAMMES
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.login.UsosLoginApi
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.ext.*

class UsosFirstLogin(val data: DataUsos, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "UsosFirstLogin"
    }

    private val api = UsosApi(data, null)

    init {
        var firstProfileId = data.loginStore.id

        UsosLoginApi(data) {
            api.apiRequest<JsonObject>(
                tag = TAG,
                service = "users/user",
                params = mapOf(
                    "fields" to listOf(
                        "id",
                        "first_name",
                        "last_name",
                        "student_number",
                        "student_programmes" to listOf(
                            "programme" to listOf("id"),
                        ),
                    ),
                ),
                responseType = UsosApi.ResponseType.OBJECT,
            ) { json, response ->
                val programmes = json.getJsonArray("student_programmes")
                if (programmes.isNullOrEmpty()) {
                    data.error(ApiError(TAG, ERROR_USOS_NO_STUDENT_PROGRAMMES)
                        .withApiResponse(json)
                        .withResponse(response))
                    return@apiRequest
                }

                val firstName = json.getString("first_name")
                val lastName = json.getString("last_name")
                val studentName = buildFullName(firstName, lastName)

                val profile = Profile(
                    id = firstProfileId++,
                    loginStoreId = data.loginStore.id,
                    loginStoreType = LoginType.USOS,
                    name = studentName,
                    subname = data.schoolId,
                    studentNameLong = studentName,
                    studentNameShort = studentName.getShortName(),
                    accountName = null, // student account
                    studentData = JsonObject(
                        "studentId" to json.getInt("id"),
                    ),
                ).also {
                    val studentNumWithoutNonDigits = json.getString("student_number")?.replace(Regex("[^0-9]"), "")
                    if (studentNumWithoutNonDigits != null && studentNumWithoutNonDigits != "") {
                        it.studentNumber = studentNumWithoutNonDigits.toInt()
                    }else{
                        it.studentNumber = -1
                    }
                    it.studentClassName = programmes.getJsonObject(0).getJsonObject("programme").getString("id")
                }

                EventBus.getDefault().postSticky(
                    FirstLoginFinishedEvent(listOf(profile), data.loginStore),
                )
                onSuccess()
            }
        }
    }
}
