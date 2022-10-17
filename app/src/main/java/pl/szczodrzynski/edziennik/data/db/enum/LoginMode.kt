/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.enum

enum class LoginMode(
    val loginType: LoginType,
    val id: Int,
) {
    MOBIDZIENNIK_WEB(LoginType.MOBIDZIENNIK, id = 0),
    LIBRUS_EMAIL(LoginType.LIBRUS, id = 0),
    LIBRUS_SYNERGIA(LoginType.LIBRUS, id = 1),
    LIBRUS_JST(LoginType.LIBRUS, id = 2),
    VULCAN_API(LoginType.VULCAN, id = 0),
    VULCAN_WEB(LoginType.VULCAN, id = 1),
    VULCAN_HEBE(LoginType.VULCAN, id = 2),
    PODLASIE_API(LoginType.PODLASIE, id = 0),
}
