/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.enums

enum class LoginMode(
    val loginType: LoginType,
    val id: Int,
) {
    MOBIDZIENNIK_WEB(LoginType.MOBIDZIENNIK, id = 100),
    LIBRUS_EMAIL(LoginType.LIBRUS, id = 200),
    LIBRUS_SYNERGIA(LoginType.LIBRUS, id = 201),
    LIBRUS_JST(LoginType.LIBRUS, id = 202),
    VULCAN_API(LoginType.VULCAN, id = 400),
    VULCAN_WEB(LoginType.VULCAN, id = 401),
    VULCAN_HEBE(LoginType.VULCAN, id = 402),
    PODLASIE_API(LoginType.PODLASIE, id = 600),
    USOS_OAUTH(LoginType.USOS, id = 700),
}
