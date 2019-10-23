package pl.szczodrzynski.edziennik.api.v2.librus.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.ERROR_NO_STUDENTS_IN_ACCOUNT
import pl.szczodrzynski.edziennik.api.v2.LIBRUS_ACCOUNTS_URL
import pl.szczodrzynski.edziennik.api.v2.LOGIN_MODE_LIBRUS_EMAIL
import pl.szczodrzynski.edziennik.api.v2.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusPortal
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginApi
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginPortal
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.data.api.AppError.CODE_LIBRUS_DISCONNECTED
import pl.szczodrzynski.edziennik.data.api.AppError.CODE_SYNERGIA_NOT_ACTIVATED
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
                portal.portalGet(TAG, LIBRUS_ACCOUNTS_URL) { json, response ->
                    val accounts = json.getJsonArray("accounts")

                    if (accounts == null || accounts.size() < 1) {
                        data.error(ApiError(TAG, ERROR_NO_STUDENTS_IN_ACCOUNT)
                                .withResponse(response)
                                .withApiResponse(json))
                        return@portalGet
                    }
                    val accountDataTime = json.getLong("lastModification")
                    val accountIds = mutableListOf<Int>()
                    val accountLogins = mutableListOf<String>()
                    val accountTokens = mutableListOf<String>()
                    val accountNamesLong = mutableListOf<String>()
                    val accountNamesShort = mutableListOf<String>()

                    for (accountEl in accounts) {
                        val account = accountEl.asJsonObject

                        val state = account.getString("state")
                        when (state) {
                            "requiring_an_action" -> CODE_LIBRUS_DISCONNECTED
                            "need-activation" -> CODE_SYNERGIA_NOT_ACTIVATED
                            else -> null
                        }?.let { errorCode ->
                            data.error(ApiError(TAG, errorCode)
                                    .withApiResponse(json)
                                    .withResponse(response))
                            return@portalGet
                        }

                        accountIds.add(account.getInt("id") ?: continue)
                        accountLogins.add(account.getString("login") ?: continue)
                        accountTokens.add(account.getString("accessToken") ?: continue)
                        accountNamesLong.add(account.getString("studentName") ?: continue)
                        val nameParts = account.getString("studentName")?.split(" ") ?: continue
                        accountNamesShort.add(nameParts[0] + " " + nameParts[1][0] + ".")
                    }

                    for (index in accountIds.indices) {
                        val newProfile = Profile()
                        newProfile.studentNameLong = accountNamesLong[index]
                        newProfile.studentNameShort = accountNamesShort[index]
                        newProfile.name = newProfile.studentNameLong
                        newProfile.subname = data.portalEmail
                        newProfile.empty = true
                        newProfile.putStudentData("accountId", accountIds[index])
                        newProfile.putStudentData("accountLogin", accountLogins[index])
                        newProfile.putStudentData("accountToken", accountTokens[index])
                        newProfile.putStudentData("accountTokenTime", (accountDataTime ?: 0) + DAY)
                        profileList.add(newProfile)
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

                }
            }
        }
    }
}