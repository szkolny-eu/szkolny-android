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
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.enums.LoginMode
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.ext.*

class LibrusFirstLogin(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LibrusFirstLogin"
    }

    private val portal = LibrusPortal(data)
    private val api = LibrusApi(data, null)
    private val profileList = mutableListOf<Profile>()

    init {
        var firstProfileId = data.loginStore.id

        if (data.loginStore.mode == LoginMode.LIBRUS_EMAIL) {
            // email login: use Portal for account list
            LibrusLoginPortal(data) {
                portal.portalGet(TAG, if (data.fakeLogin) FAKE_LIBRUS_ACCOUNTS else LIBRUS_ACCOUNTS_URL) { json, response ->
                    val accounts = json.getJsonArray("accounts")

                    if (accounts == null || accounts.size() < 1) {
                        EventBus.getDefault().postSticky(FirstLoginFinishedEvent(listOf(), data.loginStore))
                        onSuccess()
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

                        val isParent = account.getString("group") == "parent"

                        val id = account.getInt("id") ?: continue
                        val login = account.getString("login") ?: continue
                        val token = account.getString("accessToken") ?: continue
                        val tokenTime = (accountDataTime ?: 0) + DAY
                        val studentNameLong = account.getString("studentName").fixName()
                        val studentNameShort = studentNameLong.getShortName()

                        val profile = Profile(
                                firstProfileId++,
                                data.loginStore.id,
                                LoginType.LIBRUS,
                                studentNameLong,
                                data.portalEmail,
                                studentNameLong,
                                studentNameShort,
                                if (isParent) studentNameLong else null /* temporarily - there is no parent name provided, only the type */
                        ).apply {
                            studentData["accountId"] = id
                            studentData["accountLogin"] = login
                            studentData["accountToken"] = token
                            studentData["accountTokenTime"] = tokenTime
                        }
                        profileList.add(profile)
                    }

                    EventBus.getDefault().postSticky(FirstLoginFinishedEvent(profileList, data.loginStore))
                    onSuccess()
                }
            }
        }
        else {
            // synergia or JST login: use Api for account info
            LibrusLoginApi(data) {
                api.apiGet(TAG, "Me") { json ->

                    val me = json.getJsonObject("Me")
                    val account = me?.getJsonObject("Account")
                    val user = me?.getJsonObject("User")

                    val login = account.getString("Login")
                    val isParent = account?.getInt("GroupId") in 5..6

                    val studentNameLong = buildFullName(user?.getString("FirstName"), user?.getString("LastName"))
                    val studentNameShort = studentNameLong.getShortName()
                    val accountNameLong = if (isParent)
                        buildFullName(account?.getString("FirstName"), account?.getString("LastName"))
                    else null

                    val profile = Profile(
                            firstProfileId++,
                            data.loginStore.id,
                            LoginType.LIBRUS,
                            studentNameLong,
                            login,
                            studentNameLong,
                            studentNameShort,
                            accountNameLong
                    ).apply {
                        studentData["isPremium"] = account?.getBoolean("IsPremium") == true || account?.getBoolean("IsPremiumDemo") == true
                        studentData["accountId"] = account.getInt("Id") ?: 0
                        studentData["accountLogin"] = data.apiLogin ?: login
                        studentData["accountPassword"] = data.apiPassword
                        studentData["accountToken"] = data.apiAccessToken
                        studentData["accountTokenTime"] = data.apiTokenExpiryTime
                        studentData["accountRefreshToken"] = data.apiRefreshToken
                    }
                    profileList.add(profile)

                    EventBus.getDefault().postSticky(FirstLoginFinishedEvent(profileList, data.loginStore))
                    onSuccess()
                }
            }
        }
    }
}
