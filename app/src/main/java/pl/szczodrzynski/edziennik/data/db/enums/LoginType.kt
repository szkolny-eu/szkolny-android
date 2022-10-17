/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.enums

import pl.szczodrzynski.edziennik.data.db.enums.FeatureType.*

private val FEATURES_BASE = listOf(
    TIMETABLE,
    AGENDA,
    GRADES,
    HOMEWORK,
)
private val FEATURES_EXTENDED = listOf(
    BEHAVIOUR,
    ATTENDANCE,
)
private val FEATURES_MESSAGES = listOf(
    MESSAGES_INBOX,
    MESSAGES_SENT,
)

enum class LoginType(
    val id: Int,
    val features: List<FeatureType>,
) {
    MOBIDZIENNIK(id = 1, features = FEATURES_BASE + FEATURES_EXTENDED + FEATURES_MESSAGES),
    LIBRUS(id = 2, features = MOBIDZIENNIK.features + ANNOUNCEMENTS),
    IDZIENNIK(id = 3, features = LIBRUS.features),
    VULCAN(id = 4, features = MOBIDZIENNIK.features),
    EDUDZIENNIK(id = 5, features = FEATURES_BASE + FEATURES_EXTENDED),
    PODLASIE(id = 6, features = FEATURES_BASE),
    DEMO(id = 20, features = listOf()),
    TEMPLATE(id = 21, features = listOf()),
}
