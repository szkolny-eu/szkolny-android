/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-19
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_NO_STUDENTS_IN_ACCOUNT
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_VULCAN
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_STUDENT_LIST
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login.VulcanLoginApi
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanFirstLogin(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        const val TAG = "VulcanFirstLogin"
    }

    private val api = VulcanApi(data)
    private val profileList = mutableListOf<Profile>()

    init {
        val loginStoreId = data.loginStore.id
        val loginStoreType = LOGIN_TYPE_VULCAN
        var firstProfileId = loginStoreId

        VulcanLoginApi(data) {
            api.apiGet(TAG, VULCAN_API_ENDPOINT_STUDENT_LIST, baseUrl = true) { json, response ->
                val students = json.getJsonArray("Data")

                if (students == null || students.isEmpty()) {
                    data.error(ApiError(TAG, ERROR_NO_STUDENTS_IN_ACCOUNT)
                            .withResponse(response)
                            .withApiResponse(json))
                    return@apiGet
                }

                students.forEach { studentEl ->
                    val student = studentEl.asJsonObject

                    val schoolSymbol = student.getString("JednostkaSprawozdawczaSymbol") ?: return@forEach
                    val schoolName = "${data.symbol}_$schoolSymbol"
                    val studentId = student.getInt("Id") ?: return@forEach
                    val studentLoginId = student.getInt("UzytkownikLoginId") ?: return@forEach
                    val studentClassId = student.getInt("IdOddzial") ?: return@forEach
                    val studentClassName = student.getString("OkresPoziom").toString() + (student.getString("OddzialSymbol") ?: return@forEach)
                    val studentSemesterId = student.getInt("IdOkresKlasyfikacyjny") ?: return@forEach
                    val studentFirstName = student.getString("Imie") ?: ""
                    val studentLastName = student.getString("Nazwisko") ?: ""
                    val studentNameLong = "$studentFirstName $studentLastName".fixName()
                    val studentNameShort = "$studentFirstName ${studentLastName[0]}.".fixName()

                    val userLogin = student.getString("UzytkownikLogin") ?: ""
                    val currentSemesterStartDate = student.getLong("OkresDataOd") ?: return@forEach
                    val currentSemesterEndDate = (student.getLong("OkresDataDo") ?: return@forEach) + 86400
                    val studentSemesterNumber = student.getInt("OkresNumer") ?: return@forEach

                    val isParent = student.getString("UzytkownikRola") == "opiekun"
                    val accountName = if (isParent)
                        student.getString("UzytkownikNazwa")?.swapFirstLastName()?.fixName()
                    else null

                    var dateSemester1Start: Date? = null
                    var dateSemester2Start: Date? = null
                    var dateYearEnd: Date? = null
                    when (studentSemesterNumber) {
                        1 -> {
                            dateSemester1Start = Date.fromMillis(currentSemesterStartDate * 1000)
                            dateSemester2Start = Date.fromMillis(currentSemesterEndDate * 1000)
                        }
                        2 -> {
                            dateSemester2Start = Date.fromMillis(currentSemesterStartDate * 1000)
                            dateYearEnd = Date.fromMillis(currentSemesterEndDate * 1000)
                        }
                    }

                    val profile = Profile(
                            firstProfileId++,
                            loginStoreId,
                            loginStoreType,
                            studentNameLong,
                            userLogin,
                            studentNameLong,
                            studentNameShort,
                            accountName
                    ).apply {
                        this.studentClassName = studentClassName
                        studentData["studentId"] = studentId
                        studentData["studentLoginId"] = studentLoginId
                        studentData["studentClassId"] = studentClassId
                        studentData["studentSemesterId"] = studentSemesterId
                        studentData["studentSemesterNumber"] = studentSemesterNumber
                        studentData["schoolSymbol"] = schoolSymbol
                        studentData["schoolName"] = schoolName
                        studentData["currentSemesterEndDate"] = currentSemesterEndDate
                    }
                    dateSemester1Start?.let {
                        profile.dateSemester1Start = it
                        profile.studentSchoolYearStart = it.year
                    }
                    dateSemester2Start?.let { profile.dateSemester2Start = it }
                    dateYearEnd?.let { profile.dateYearEnd = it }

                    profileList.add(profile)
                }

                EventBus.getDefault().post(FirstLoginFinishedEvent(profileList, data.loginStore))
                onSuccess()
            }
        }
    }
}
