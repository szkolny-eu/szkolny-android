/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2

import pl.szczodrzynski.edziennik.api.v2.idziennik.login.IdziennikLoginApi
import pl.szczodrzynski.edziennik.api.v2.idziennik.login.IdziennikLoginWeb
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginApi
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginMessages
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginPortal
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLoginSynergia
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.login.MobidziennikLoginWeb
import pl.szczodrzynski.edziennik.api.v2.models.LoginMethod
import pl.szczodrzynski.edziennik.api.v2.template.login.TemplateLoginApi
import pl.szczodrzynski.edziennik.api.v2.template.login.TemplateLoginWeb
import pl.szczodrzynski.edziennik.api.v2.vulcan.login.VulcanLoginApi

// librus
// mobidziennik
// idziennik
// vulcan
// mobireg

const val SYNERGIA_API_ENABLED = true



const val LOGIN_TYPE_IDZIENNIK = 3

const val LOGIN_TYPE_TEMPLATE = 21

// LOGIN MODES
const val LOGIN_MODE_IDZIENNIK_WEB = 0

const val LOGIN_MODE_TEMPLATE_WEB = 0

// LOGIN METHODS
const val LOGIN_METHOD_NOT_NEEDED = -1
const val LOGIN_METHOD_IDZIENNIK_WEB = 100
const val LOGIN_METHOD_IDZIENNIK_API = 200
const val LOGIN_METHOD_TEMPLATE_WEB = 100
const val LOGIN_METHOD_TEMPLATE_API = 200

const val LOGIN_TYPE_LIBRUS = 2
const val LOGIN_MODE_LIBRUS_EMAIL = 0
const val LOGIN_MODE_LIBRUS_SYNERGIA = 1
const val LOGIN_MODE_LIBRUS_JST = 2
const val LOGIN_METHOD_LIBRUS_PORTAL = 100
const val LOGIN_METHOD_LIBRUS_API = 200
const val LOGIN_METHOD_LIBRUS_SYNERGIA = 300
const val LOGIN_METHOD_LIBRUS_MESSAGES = 400
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
                .withIsPossible { _, loginStore -> !loginStore.hasLoginData("fakeLogin") }
                .withRequiredLoginMethod { profile, _ ->
                    if (profile?.hasStudentData("accountPassword") == false) LOGIN_METHOD_LIBRUS_API else LOGIN_METHOD_NOT_NEEDED
                },

        LoginMethod(LOGIN_TYPE_LIBRUS, LOGIN_METHOD_LIBRUS_MESSAGES, LibrusLoginMessages::class.java)
                .withIsPossible { _, loginStore -> !loginStore.hasLoginData("fakeLogin") }
                .withRequiredLoginMethod { profile, _ ->
                    if (profile?.hasStudentData("accountPassword") == false) LOGIN_METHOD_LIBRUS_SYNERGIA else LOGIN_METHOD_NOT_NEEDED
                }
)

const val LOGIN_TYPE_MOBIDZIENNIK = 1
const val LOGIN_MODE_MOBIDZIENNIK_WEB = 0
const val LOGIN_METHOD_MOBIDZIENNIK_WEB = 100
const val LOGIN_METHOD_MOBIDZIENNIK_API2 = 300
val mobidziennikLoginMethods = listOf(
        LoginMethod(LOGIN_TYPE_MOBIDZIENNIK, LOGIN_METHOD_MOBIDZIENNIK_WEB, MobidziennikLoginWeb::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_NOT_NEEDED }/*,

        LoginMethod(LOGIN_TYPE_MOBIDZIENNIK, LOGIN_METHOD_MOBIDZIENNIK_API2, MobidziennikLoginApi2::class.java)
                .withIsPossible { _, loginStore -> loginStore.hasLoginData("email") }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_NOT_NEEDED }*/
)

const val LOGIN_TYPE_VULCAN = 4
const val LOGIN_MODE_VULCAN_API = 0
const val LOGIN_MODE_VULCAN_WEB = 1
const val LOGIN_METHOD_VULCAN_WEB_MAIN = 100
const val LOGIN_METHOD_VULCAN_WEB_NEW = 200
const val LOGIN_METHOD_VULCAN_WEB_OLD = 300
const val LOGIN_METHOD_VULCAN_WEB_MESSAGES = 400
const val LOGIN_METHOD_VULCAN_API = 500
val vulcanLoginMethods = listOf(
        /*LoginMethod(LOGIN_TYPE_VULCAN, LOGIN_METHOD_VULCAN_WEB_MAIN, VulcanLoginWebMain::class.java)
                .withIsPossible { _, _ -> false }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_NOT_NEEDED },

        LoginMethod(LOGIN_TYPE_VULCAN, LOGIN_METHOD_VULCAN_WEB_NEW, VulcanLoginWebNew::class.java)
                .withIsPossible { _, _ -> false }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_VULCAN_WEB_MAIN },

        LoginMethod(LOGIN_TYPE_VULCAN, LOGIN_METHOD_VULCAN_WEB_OLD, VulcanLoginWebOld::class.java)
                .withIsPossible { _, _ -> false }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_VULCAN_WEB_MAIN },*/

        LoginMethod(LOGIN_TYPE_VULCAN, LOGIN_METHOD_VULCAN_API, VulcanLoginApi::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { _, loginStore ->
                    if (loginStore.mode == LOGIN_MODE_VULCAN_WEB) LOGIN_METHOD_VULCAN_WEB_NEW else LOGIN_METHOD_NOT_NEEDED
                }
)

val idziennikLoginMethods = listOf(
        LoginMethod(LOGIN_TYPE_IDZIENNIK, LOGIN_METHOD_IDZIENNIK_WEB, IdziennikLoginWeb::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_NOT_NEEDED },

        LoginMethod(LOGIN_TYPE_IDZIENNIK, LOGIN_METHOD_IDZIENNIK_API, IdziennikLoginApi::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_IDZIENNIK_WEB }
)

val templateLoginMethods = listOf(
        LoginMethod(LOGIN_TYPE_TEMPLATE, LOGIN_METHOD_TEMPLATE_WEB, TemplateLoginWeb::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_NOT_NEEDED },

        LoginMethod(LOGIN_TYPE_TEMPLATE, LOGIN_METHOD_TEMPLATE_API, TemplateLoginApi::class.java)
                .withIsPossible { _, _ -> true }
                .withRequiredLoginMethod { _, _ -> LOGIN_METHOD_TEMPLATE_WEB }
)