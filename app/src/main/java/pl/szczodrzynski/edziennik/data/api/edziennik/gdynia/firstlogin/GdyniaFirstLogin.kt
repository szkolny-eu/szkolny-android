/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-17
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.firstlogin

import org.greenrobot.eventbus.EventBus
import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.GDYNIA_WEB_DISPLAY
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_GDYNIA
import pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.DataGdynia
import pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.data.GdyniaWeb
import pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.login.GdyniaLoginWeb
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.db.entity.Profile

class GdyniaFirstLogin(val data: DataGdynia, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "GdyniaFirstLogin"
    }

    private val web = GdyniaWeb(data, null)
    private val profileList = mutableListOf<Profile>()

    init {
        val loginStoreId = data.loginStore.id
        val loginStoreType = LOGIN_TYPE_GDYNIA

        GdyniaLoginWeb(data) {
            web.webGet(TAG, GDYNIA_WEB_DISPLAY, parameters = mapOf(
                    "form" to "zmiana_danych"
            )) { html ->
                run {
                    val doc = Jsoup.parse(html)

                    val firstName = doc.selectFirst("#f_imie")?.`val`() ?: return@run
                    val lastName = doc.selectFirst("#f_nazwisko")?.`val`() ?: return@run
                    val studentNameLong = "$firstName $lastName".fixName()
                    val studentNameShort = studentNameLong.getShortName()

                    val login = doc.selectFirst("#f_login").`val`().nullIfBlank()
                    val alias = doc.selectFirst("#f_kod_logowania").`val`().nullIfBlank()
                    val email = doc.selectFirst("#f_email")?.`val`().nullIfBlank()

                    val subname = alias ?: email ?: data.loginUsername

                    val profile = Profile(
                            loginStoreId,
                            loginStoreId,
                            loginStoreType,
                            studentNameLong,
                            subname,
                            studentNameLong,
                            studentNameShort,
                            null
                    ).apply {
                        studentData["studentLogin"] = login
                        studentData["studentAlias"] = alias
                        studentData["studentEmail"] = email
                    }

                    profileList.add(profile)
                }

                EventBus.getDefault().postSticky(FirstLoginFinishedEvent(profileList, data.loginStore))
                onSuccess()
            }
        }
    }
}
