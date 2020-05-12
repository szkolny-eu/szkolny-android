/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_PODLASIE
import pl.szczodrzynski.edziennik.data.api.PODLASIE_API_USER_ENDPOINT
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.PodlasieApi
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.login.PodlasieLoginApi
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.db.entity.Profile

class PodlasieFirstLogin(val data: DataPodlasie, val onSuccess: () -> Unit) {
    companion object {
        const val TAG = "PodlasieFirstLogin"
    }

    private val api = PodlasieApi(data, null)

    init {
        val loginStoreId = data.loginStore.id
        val loginStoreType = LOGIN_TYPE_PODLASIE

        PodlasieLoginApi(data) {
            api.apiGet(TAG, PODLASIE_API_USER_ENDPOINT) { json ->
                val uuid = json.getString("Uuid")
                val login = json.getString("Login")
                val firstName = json.getString("FirstName")
                val lastName = json.getString("LastName")
                val studentNameLong = "$firstName $lastName".fixName()
                val studentNameShort = studentNameLong.getShortName()
                val schoolName = json.getString("SchoolName")
                val className = json.getString("SchoolClass")
                val schoolYear = json.getString("ActualSchoolYear")?.replace(' ', '/')
                val semester = json.getString("ActualTermShortcut")?.length
                val apiUrl = json.getString("URL")

                val profile = Profile(
                        loginStoreId,
                        loginStoreId,
                        loginStoreType,
                        studentNameLong,
                        login,
                        studentNameLong,
                        studentNameShort,
                        null
                ).apply {
                    studentData["studentId"] = uuid
                    studentData["studentLogin"] = login
                    studentData["schoolName"] = schoolName
                    studentData["className"] = className
                    studentData["schoolYear"] = schoolYear
                    studentData["currentSemester"] = semester ?: 1
                    studentData["apiUrl"] = apiUrl

                    schoolYear?.split('/')?.get(0)?.toInt()?.let {
                        studentSchoolYearStart = it
                    }
                }

                EventBus.getDefault().postSticky(FirstLoginFinishedEvent(listOf(profile), data.loginStore))
                onSuccess()
            }
        }
    }
}
