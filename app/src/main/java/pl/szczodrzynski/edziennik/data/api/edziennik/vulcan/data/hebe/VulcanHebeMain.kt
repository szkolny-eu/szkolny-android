/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-20.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import com.google.gson.JsonArray
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_VULCAN
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MAIN
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_MAIN
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanHebeMain(
    override val data: DataVulcan,
    override val lastSync: Long? = null
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeMain"
    }

    fun getStudents(
        profile: Profile?,
        profileList: MutableList<Profile>?,
        loginStoreId: Int? = null,
        firstProfileId: Int? = null,
        onEmpty: (() -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        if (profile == null && (profileList == null || loginStoreId == null || firstProfileId == null))
            throw IllegalArgumentException()

        apiGet(
            TAG,
            VULCAN_HEBE_ENDPOINT_MAIN,
            query = mapOf("lastSyncDate" to "null"),
            baseUrl = profile == null
        ) { students: JsonArray, _ ->
            if (students.isEmpty()) {
                if (onEmpty != null)
                    onEmpty()
                else
                    onSuccess()
                return@apiGet
            }

            // safe to assume this will be non-null when creating a profile
            var profileId = firstProfileId ?: loginStoreId ?: 1

            students.forEach { studentEl ->
                val student = studentEl.asJsonObject

                val pupil = student.getJsonObject("Pupil")
                val studentId = pupil.getInt("Id") ?: return@forEach

                // check the student ID in case of not first login
                if (profile != null && data.studentId != studentId)
                    return@forEach

                val unit = student.getJsonObject("Unit")
                val constituentUnit = student.getJsonObject("ConstituentUnit")
                val login = student.getJsonObject("Login")
                val periods = student.getJsonArray("Periods")?.map {
                    it.asJsonObject
                } ?: listOf()

                val period = periods.firstOrNull {
                    it.getBoolean("Current", false)
                } ?: return@forEach

                val periodLevel = period.getInt("Level") ?: return@forEach
                val semester1 = periods.firstOrNull {
                    it.getInt("Level") == periodLevel && it.getInt("Number") == 1
                }
                val semester2 = periods.firstOrNull {
                    it.getInt("Level") == periodLevel && it.getInt("Number") == 2
                }

                val schoolSymbol = unit.getString("Symbol") ?: return@forEach
                val schoolShort = constituentUnit.getString("Short") ?: return@forEach
                val schoolCode = "${data.symbol}_$schoolSymbol"

                val studentUnitId = unit.getInt("Id") ?: return@forEach
                val studentConstituentId = constituentUnit.getInt("Id") ?: return@forEach
                val studentLoginId = login.getInt("Id") ?: return@forEach
                //val studentClassId = student.getInt("IdOddzial") ?: return@forEach
                val studentClassName = student.getString("ClassDisplay") ?: return@forEach
                val studentFirstName = pupil.getString("FirstName") ?: ""
                val studentLastName = pupil.getString("Surname") ?: ""
                val studentNameLong = "$studentFirstName $studentLastName".fixName()
                val studentNameShort = "$studentFirstName ${studentLastName[0]}.".fixName()
                val userLogin = login.getString("Value") ?: ""

                val studentSemesterId = period.getInt("Id") ?: return@forEach
                val studentSemesterNumber = period.getInt("Number") ?: return@forEach

                val hebeContext = student.getString("Context")

                val isParent = login.getString("LoginRole").equals("opiekun", ignoreCase = true)
                val accountName = if (isParent)
                    login.getString("DisplayName")?.fixName()
                else null

                val dateSemester1Start = semester1
                    ?.getJsonObject("Start")
                    ?.getString("Date")
                    ?.let { Date.fromY_m_d(it) }
                val dateSemester2Start = semester2
                    ?.getJsonObject("Start")
                    ?.getString("Date")
                    ?.let { Date.fromY_m_d(it) }
                val dateYearEnd = semester2
                    ?.getJsonObject("End")
                    ?.getString("Date")
                    ?.let { Date.fromY_m_d(it) }

                val newProfile = profile ?: Profile(
                    profileId++,
                    loginStoreId!!,
                    LOGIN_TYPE_VULCAN,
                    studentNameLong,
                    userLogin,
                    studentNameLong,
                    studentNameShort,
                    accountName
                )

                newProfile.apply {
                    this.studentClassName = studentClassName
                    studentData["symbol"] = data.symbol

                    studentData["studentId"] = studentId
                    studentData["studentUnitId"] = studentUnitId
                    studentData["studentConstituentId"] = studentConstituentId
                    studentData["studentLoginId"] = studentLoginId
                    studentData["studentSemesterId"] = studentSemesterId
                    studentData["studentSemesterNumber"] = studentSemesterNumber
                    studentData["semester1Id"] = semester1?.getInt("Id") ?: 0
                    studentData["semester2Id"] = semester2?.getInt("Id") ?: 0
                    studentData["schoolSymbol"] = schoolSymbol
                    studentData["schoolShort"] = schoolShort
                    studentData["schoolName"] = schoolCode
                    studentData["hebeContext"] = hebeContext
                }
                dateSemester1Start?.let {
                    newProfile.dateSemester1Start = it
                    newProfile.studentSchoolYearStart = it.year
                }
                dateSemester2Start?.let { newProfile.dateSemester2Start = it }
                dateYearEnd?.let { newProfile.dateYearEnd = it }

                if (profile != null)
                    data.setSyncNext(ENDPOINT_VULCAN_HEBE_MAIN, 1 * DAY)

                profileList?.add(newProfile)
            }

            onSuccess()
        }
    }
}
