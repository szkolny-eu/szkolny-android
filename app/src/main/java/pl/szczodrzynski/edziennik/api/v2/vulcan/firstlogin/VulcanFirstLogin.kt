/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-19
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.api.v2.ERROR_NO_STUDENTS_IN_ACCOUNT
import pl.szczodrzynski.edziennik.api.v2.VULCAN_API_ENDPOINT_STUDENT_LIST
import pl.szczodrzynski.edziennik.api.v2.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.api.v2.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.api.v2.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.api.v2.vulcan.login.VulcanLoginApi
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanFirstLogin(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        const val TAG = "VulcanFirstLogin"
    }

    private val api = VulcanApi(data)
    private val profileList = mutableListOf<Profile>()

    init {
        VulcanLoginApi(data) {
            api.apiGet(TAG, VULCAN_API_ENDPOINT_STUDENT_LIST, baseUrl = true) { json, response ->
                val students = json.getJsonArray("Data")

                if (students == null || students.size() < 1) {
                    data.error(ApiError(TAG, ERROR_NO_STUDENTS_IN_ACCOUNT)
                            .withResponse(response)
                            .withApiResponse(json))
                    return@apiGet
                }

                students.forEach { studentEl ->
                    val student = studentEl.asJsonObject

                    val studentId = student.getInt("Id") ?: return@forEach
                    val studentLoginId = student.getInt("UzytkownikLoginId") ?: return@forEach
                    val studentClassId = student.getInt("IdOddzial") ?: return@forEach
                    val studentClassNumber = student.getString("OkresPoziom")
                    val studentClassSymbol = student.getString("OddzialSymbol")
                    val studentClassName = "$studentClassNumber$studentClassSymbol"
                    val studentSemesterId = student.getInt("IdOkresKlasyfikacyjny")
                            ?: return@forEach
                    val studentFirstName = student.getString("Imie")
                    val studentLastName = student.getString("Nazwisko")
                    val studentNameLong = "$studentFirstName $studentLastName"
                    val studentNameShort = "$studentFirstName ${studentLastName?.get(0)}."
                    val userName = student.getString("UzytkownikNazwa") ?: ""
                    val userLogin = student.getString("UzytkownikLogin") ?: ""
                    val schoolSymbol = student.getString("JednostkaSprawozdawczaSymbol")
                            ?: return@forEach
                    val schoolName = "${data.symbol}_$schoolSymbol"
                    val currentSemesterStartDate = student.getInt("OkresDataOd") ?: return@forEach
                    val currentSemesterEndDate = (student.getInt("OkresDataDo")
                            ?: return@forEach) + 86400
                    val studentSemesterNumber = student.getInt("OkresNumer") ?: return@forEach

                    val newProfile = Profile()
                    newProfile.empty = true

                    newProfile.putStudentData("studentId", studentId)
                    newProfile.putStudentData("studentLoginId", studentLoginId)
                    newProfile.putStudentData("studentClassId", studentClassId)
                    newProfile.putStudentData("studentClassName", studentClassName)
                    newProfile.putStudentData("studentSemesterId", studentSemesterId)
                    newProfile.putStudentData("userName", userName)
                    newProfile.putStudentData("schoolSymbol", schoolSymbol)
                    newProfile.putStudentData("schoolName", schoolName)
                    newProfile.putStudentData("currentSemesterEndDate", currentSemesterEndDate)
                    newProfile.putStudentData("studentSemesterNumber", studentSemesterNumber)

                    when (studentSemesterNumber) {
                        1 -> {
                            newProfile.dateSemester1Start = Date.fromMillis((currentSemesterStartDate * 1000).toLong())
                            newProfile.dateSemester2Start = Date.fromMillis((currentSemesterEndDate * 1000).toLong())
                        }
                        2 -> {
                            newProfile.dateSemester2Start = Date.fromMillis((currentSemesterStartDate * 1000).toLong())
                            newProfile.dateYearEnd = Date.fromMillis((currentSemesterEndDate * 1000).toLong())
                        }
                    }

                    newProfile.studentNameLong = studentNameLong
                    newProfile.studentNameShort = studentNameShort
                    newProfile.name = studentNameLong
                    newProfile.subname = userLogin

                    profileList.add(newProfile)
                }

                EventBus.getDefault().post(FirstLoginFinishedEvent(profileList, data.loginStore))
                onSuccess()
            }
        }
    }
}
