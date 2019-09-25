/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2

import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginLibrusPortal
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginLibrusApi
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginLibrusMessages
import pl.szczodrzynski.edziennik.api.v2.librus.login.LoginLibrusSynergia
import pl.szczodrzynski.edziennik.api.v2.models.LoginMethod

const val LOGIN_TYPE_MOBIDZIENNIK = 1
const val LOGIN_TYPE_LIBRUS = 2
const val LOGIN_TYPE_IUCZNIOWIE = 3
const val LOGIN_TYPE_VULCAN = 4
const val LOGIN_TYPE_DEMO = 20

// LOGIN MODES
const val LOGIN_MODE_LIBRUS_EMAIL = 0
const val LOGIN_MODE_LIBRUS_SYNERGIA = 1
const val LOGIN_MODE_LIBRUS_JST = 2
const val LOGIN_MODE_MOBIDZIENNIK_WEB = 0
const val LOGIN_MODE_IDZIENNIK_WEB = 0
const val LOGIN_MODE_VULCAN_WEB = 0

// LOGIN METHODS
const val LOGIN_METHOD_NOT_NEEDED = -1
const val LOGIN_METHOD_LIBRUS_PORTAL = 100
const val LOGIN_METHOD_LIBRUS_API = 200
const val LOGIN_METHOD_LIBRUS_SYNERGIA = 300
const val LOGIN_METHOD_LIBRUS_MESSAGES = 400
const val LOGIN_METHOD_MOBIDZIENNIK_API = 100
const val LOGIN_METHOD_IDZIENNIK_WEB = 100
const val LOGIN_METHOD_IDZIENNIK_API = 200
const val LOGIN_METHOD_VULCAN_WEB = 100
const val LOGIN_METHOD_VULCAN_API = 200

val librusLoginMethods = listOf(
        LoginMethod(LOGIN_TYPE_LIBRUS, LOGIN_METHOD_LIBRUS_PORTAL, null, LoginLibrusPortal::class.java) { _, _ -> LOGIN_METHOD_NOT_NEEDED },
        LoginMethod(LOGIN_TYPE_LIBRUS, LOGIN_METHOD_LIBRUS_API, null, LoginLibrusApi::class.java) { _, loginStore ->
            if (loginStore.mode == LOGIN_MODE_LIBRUS_EMAIL) LOGIN_METHOD_LIBRUS_PORTAL else LOGIN_METHOD_NOT_NEEDED
        },
        LoginMethod(LOGIN_TYPE_LIBRUS, LOGIN_METHOD_LIBRUS_SYNERGIA, listOf(FEATURE_GRADES, FEATURE_HOMEWORKS, FEATURE_MESSAGES_INBOX, FEATURE_MESSAGES_OUTBOX), LoginLibrusSynergia::class.java) { profile, _ ->
            if (profile?.hasStudentData("accountPassword") == false) LOGIN_METHOD_LIBRUS_API else LOGIN_METHOD_NOT_NEEDED
        },
        LoginMethod(LOGIN_TYPE_LIBRUS, LOGIN_METHOD_LIBRUS_MESSAGES, listOf(FEATURE_MESSAGES_INBOX, FEATURE_MESSAGES_OUTBOX), LoginLibrusMessages::class.java) { profile, _ ->
            if (profile?.hasStudentData("accountPassword") == false) LOGIN_METHOD_LIBRUS_SYNERGIA else LOGIN_METHOD_NOT_NEEDED
        }
)