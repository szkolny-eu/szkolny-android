package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_MOBIDZIENNIK
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login.MobidziennikLoginWeb
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.set
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikFirstLogin(val data: DataMobidziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikFirstLogin"
    }

    private val web = MobidziennikWeb(data)
    private val profileList = mutableListOf<Profile>()

    init {
        val loginStoreId = data.loginStore.id
        val loginStoreType = LOGIN_TYPE_MOBIDZIENNIK
        var firstProfileId = loginStoreId

        MobidziennikLoginWeb(data) {
            web.webGet(TAG, "/api/zrzutbazy") { text ->
                val tables = text.split("T@B#LA")

                val accountNameLong = run {
                    tables[0]
                            .split("\n")
                            .map { it.split("|") }
                            .singleOrNull { it.getOrNull(1) != "*" }
                            ?.let {
                                "${it[4]} ${it[5]}".fixName()
                            }
                }

                var dateSemester1Start: Date? = null
                var dateSemester2Start: Date? = null
                var dateYearEnd: Date? = null
                for (row in tables[3].split("\n")) {
                    if (row.isEmpty())
                        continue
                    val cols = row.split("|")

                    when (cols[1]) {
                        "semestr1_poczatek" -> dateSemester1Start = Date.fromYmd(cols[3])
                        "semestr2_poczatek" -> dateSemester2Start = Date.fromYmd(cols[3])
                        "koniec_roku_szkolnego" -> dateYearEnd = Date.fromYmd(cols[3])
                    }
                }

                tables[8].split("\n").forEach { student ->
                    if (student.isEmpty())
                        return@forEach
                    val student1 = student.split("|")
                    if (student1.size == 2)
                        return@forEach

                    val studentNameLong = "${student1[2]} ${student1[4]}".fixName()
                    val studentNameShort = "${student1[2]} ${student1[4][0]}.".fixName()
                    val accountName = if (accountNameLong == studentNameLong) null else accountNameLong

                    val profile = Profile(
                            firstProfileId++,
                            loginStoreId,
                            loginStoreType,
                            studentNameLong,
                            data.loginUsername,
                            studentNameLong,
                            studentNameShort,
                            accountName
                    ).apply {
                        studentData["studentId"] = student1[0].toInt()
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
