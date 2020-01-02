package pl.szczodrzynski.edziennik.data.api.edziennik.librus.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusPortal
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.login.LibrusLoginApi
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.login.LibrusLoginPortal
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

class LibrusFirstLogin(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LibrusFirstLogin"
    }

    private val portal = LibrusPortal(data)
    private val api = LibrusApi(data)
    private val profileList = mutableListOf<Profile>()

    init {
        if (data.loginStore.mode == LOGIN_MODE_LIBRUS_EMAIL) {
            // email login: use Portal for account list
            LibrusLoginPortal(data) {
                portal.portalGet(TAG, if (data.fakeLogin) FAKE_LIBRUS_ACCOUNTS else LIBRUS_ACCOUNTS_URL) { json, response ->
                    val accounts = json.getJsonArray("accounts")

                    if (accounts == null || accounts.size() < 1) {
                        data.error(ApiError(TAG, ERROR_NO_STUDENTS_IN_ACCOUNT)
                                .withResponse(response)
                                .withApiResponse(json))
                        return@portalGet
                    }
                    val accountDataTime = json.getLong("lastModification")

                    for (accountEl in accounts) {
                        val account = accountEl.asJsonObject

                        val state = account.getString("state")
                        when (state) {
                            "requiring_an_action" -> ERROR_LIBRUS_PORTAL_SYNERGIA_DISCONNECTED
                            "need-activation" -> ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED
                            else -> null
                        }?.let { errorCode ->
                            data.error(ApiError(TAG, errorCode)
                                    .withApiResponse(json)
                                    .withResponse(response))
                            return@portalGet
                        }

                        val id = account.getInt("id") ?: continue
                        val login = account.getString("login") ?: continue
                        val token = account.getString("accessToken") ?: continue
                        val tokenTime = (accountDataTime ?: 0) + DAY
                        val name = account.getString("studentName")?.fixName() ?: ""

                        val profile = Profile()
                        profile.studentNameLong = name
                        profile.studentNameShort = name.getShortName()
                        profile.name = profile.studentNameLong
                        profile.subname = data.portalEmail
                        profile.empty = true
                        profile.putStudentData("accountId", id)
                        profile.putStudentData("accountLogin", login)
                        profile.putStudentData("accountToken", token)
                        profile.putStudentData("accountTokenTime", tokenTime)
                        profileList.add(profile)
                    }

                    EventBus.getDefault().post(FirstLoginFinishedEvent(profileList, data.loginStore))
                    onSuccess()
                }
            }
        }
        else {
            // synergia or JST login: use Api for account info
            LibrusLoginApi(data) {
                api.apiGet(TAG, "Me") { json ->

                    val profile = Profile()

                    val me = json.getJsonObject("Me")
                    val account = me?.getJsonObject("Account")
                    val user = me?.getJsonObject("User")

                    profile.putStudentData("isPremium", account?.getBoolean("IsPremium") == true || account?.getBoolean("IsPremiumDemo") == true)

                    val isParent = account?.getInt("GroupId") == 5
                    profile.accountNameLong =
                            if (isParent)
                                buildFullName(account?.getString("FirstName"), account?.getString("LastName"))
                            else null

                    profile.studentNameLong =
                            buildFullName(user?.getString("FirstName"), user?.getString("LastName"))

                    profile.studentNameShort = profile.studentNameLong?.getShortName()
                    profile.name = profile.studentNameLong
                    profile.subname = account.getString("Login")
                    profile.empty = true
                    profile.putStudentData("accountId", account.getInt("Id") ?: 0)
                    profile.putStudentData("accountLogin", profile.subname)
                    profile.putStudentData("accountToken", data.apiAccessToken)
                    profile.putStudentData("accountRefreshToken", data.apiRefreshToken)
                    profile.putStudentData("accountTokenTime", data.apiTokenExpiryTime)
                    profileList.add(profile)

                    EventBus.getDefault().post(FirstLoginFinishedEvent(profileList, data.loginStore))
                    onSuccess()
                }
            }
        }
    }
}
