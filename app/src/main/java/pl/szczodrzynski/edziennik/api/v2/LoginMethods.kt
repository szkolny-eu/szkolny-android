/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2

import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginPortal
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginApi
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginMessages
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginSynergia
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.login.MobidziennikLoginWeb
import pl.szczodrzynski.edziennik.api.v2.models.LoginMethod
import pl.szczodrzynski.edziennik.api.v2.template.login.TemplateLoginApi
import pl.szczodrzynski.edziennik.api.v2.template.login.TemplateLoginWeb

// librus
// mobidziennik
// idziennik
// vulcan
// mobireg

const val SYNERGIA_API_ENABLED = true

const val LOGIN_TYPE_LIBRUS = 2
const val LOGIN_TYPE_MOBIDZIENNIK = 1
const val LOGIN_TYPE_IDZIENNIK = 3
const val LOGIN_TYPE_VULCAN = 4
const val LOGIN_TYPE_TEMPLATE = 21

// LOGIN MODES
const val LOGIN_MODE_LIBRUS_EMAIL = 0
const val LOGIN_MODE_LIBRUS_SYNERGIA = 1
const val LOGIN_MODE_LIBRUS_JST = 2
const val LOGIN_MODE_MOBIDZIENNIK_WEB = 0
const val LOGIN_MODE_IDZIENNIK_WEB = 0
const val LOGIN_MODE_VULCAN_WEB = 0
const val LOGIN_MODE_TEMPLATE_WEB = 0

// LOGIN METHODS
const val LOGIN_METHOD_NOT_NEEDED = -1
const val LOGIN_METHOD_LIBRUS_PORTAL = 100
const val LOGIN_METHOD_LIBRUS_API = 200
const val LOGIN_METHOD_LIBRUS_SYNERGIA = 300
const val LOGIN_METHOD_LIBRUS_MESSAGES = 400
const val LOGIN_METHOD_MOBIDZIENNIK_WEB = 100
const val LOGIN_METHOD_MOBIDZIENNIK_API2 = 300
const val LOGIN_METHOD_IDZIENNIK_WEB = 100
const val LOGIN_METHOD_IDZIENNIK_API = 200
const val LOGIN_METHOD_VULCAN_WEB = 100
const val LOGIN_METHOD_VULCAN_API = 200
const val LOGIN_METHOD_TEMPLATE_WEB = 100
const val LOGIN_METHOD_TEMPLATE_API = 200

val librusLoginMethods = listOf(
        LoginMethod(LOGIN_TYPE_LIBRUS, LOGIN_METHOD_LIBRUS_PORTAL, LibrusLoginPortal::class.java)
                .withIsPossible { _, loginStore ->
                    loginStore.mode == LOGIN_MODE_LIBRUS_EMAIL
                }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_NOT_NEEDED },

        LoginMethod(LOGIN_TYPE_LIBRUS, LOGIN_METHOD_LIBRUS_API, LibrusLoginApi::class.java)
                .withIsPossible { _, loginStore ->
                    loginStore.mode != LOGIN_MODE_LIBRUS_SYNERGIA || SYNERGIA_API_ENABLED
                }
                .withRequiredLoginMethod { _, loginStore ->
                    if (loginStore.mode == LOGIN_MODE_LIBRUS_EMAIL) LOGIN_METHOD_LIBRUS_PORTAL else LOGIN_METHOD_NOT_NEEDED
                },

        LoginMethod(LOGIN_TYPE_LIBRUS, LOGIN_METHOD_LIBRUS_SYNERGIA, LibrusLoginSynergia::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { profile, _ ->
                    if (profile?.hasStudentData("accountPassword") == false) LOGIN_METHOD_LIBRUS_API else LOGIN_METHOD_NOT_NEEDED
                },

        LoginMethod(LOGIN_TYPE_LIBRUS, LOGIN_METHOD_LIBRUS_MESSAGES, LibrusLoginMessages::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { profile, _ ->
                    if (profile?.hasStudentData("accountPassword") == false) LOGIN_METHOD_LIBRUS_SYNERGIA else LOGIN_METHOD_NOT_NEEDED
                }
)

val mobidziennikLoginMethods = listOf(
        LoginMethod(LOGIN_TYPE_MOBIDZIENNIK, LOGIN_METHOD_MOBIDZIENNIK_WEB, MobidziennikLoginWeb::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_NOT_NEEDED }
)

val templateLoginMethods = listOf(
        LoginMethod(LOGIN_TYPE_TEMPLATE, LOGIN_METHOD_TEMPLATE_WEB, TemplateLoginWeb::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_NOT_NEEDED },

        LoginMethod(LOGIN_TYPE_TEMPLATE, LOGIN_METHOD_TEMPLATE_API, TemplateLoginApi::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_TEMPLATE_WEB }
)