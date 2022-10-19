/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.enums

import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile

enum class LoginMethod(
    val loginType: LoginType,
    val id: Int,
    val isPossible: ((
        profile: Profile?,
        loginStore: LoginStore,
    ) -> Boolean)? = null,
    val requiredLoginMethod: ((
        profile: Profile?,
        loginStore: LoginStore,
    ) -> LoginMethod?)? = null,
) {
    MOBIDZIENNIK_WEB(
        loginType = LoginType.MOBIDZIENNIK,
        id = 100,
    ),
    MOBIDZIENNIK_API2(
        loginType = LoginType.MOBIDZIENNIK,
        id = 300,
        isPossible = { profile, _ -> profile?.studentData?.has("email") ?: false },
    ),
    LIBRUS_PORTAL(
        loginType = LoginType.LIBRUS,
        id = 100,
        isPossible = { _, loginStore -> loginStore.mode == LoginMode.LIBRUS_EMAIL },
    ),
    LIBRUS_API(
        loginType = LoginType.LIBRUS,
        id = 200,
        isPossible = { _, loginStore -> loginStore.mode != LoginMode.LIBRUS_SYNERGIA },
        requiredLoginMethod = { _, loginStore ->
            if (loginStore.mode == LoginMode.LIBRUS_EMAIL) LIBRUS_PORTAL
            else null
        },
    ),
    LIBRUS_SYNERGIA(
        loginType = LoginType.LIBRUS,
        id = 300,
        isPossible = { _, loginStore -> !loginStore.hasLoginData("fakeLogin") },
        requiredLoginMethod = { _, _ -> LIBRUS_API },
    ),
    LIBRUS_MESSAGES(
        loginType = LoginType.LIBRUS,
        id = 400,
        isPossible = { _, loginStore -> !loginStore.hasLoginData("fakeLogin") },
        requiredLoginMethod = { _, _ -> LIBRUS_SYNERGIA },
    ),
    VULCAN_WEB_MAIN(
        loginType = LoginType.VULCAN,
        id = 100,
        isPossible = { _, loginStore -> loginStore.hasLoginData("webHost") },
    ),
    VULCAN_HEBE(
        loginType = LoginType.VULCAN,
        id = 600,
        isPossible = { _, loginStore -> loginStore.mode != LoginMode.VULCAN_API },
    ),
    PODLASIE_API(
        loginType = LoginType.PODLASIE,
        id = 100,
    ),
    USOS_API(
        loginType = LoginType.USOS,
        id = 100,
    ),
    TEMPLATE_WEB(
        loginType = LoginType.TEMPLATE,
        id = 100,
    ),
    TEMPLATE_API(
        loginType = LoginType.TEMPLATE,
        id = 200,
        requiredLoginMethod = { _, _ -> TEMPLATE_WEB },
    ),
}
