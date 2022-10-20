/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-21.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.getIntList
import pl.szczodrzynski.edziennik.config.utils.set
import pl.szczodrzynski.edziennik.data.db.enums.NotificationType
import pl.szczodrzynski.edziennik.ext.asNotificationTypeOrNull

class ProfileConfigSync(private val config: ProfileConfig) {
    private var mNotificationFilter: List<NotificationType>? = null
    var notificationFilter: List<NotificationType>
        get() { mNotificationFilter = mNotificationFilter ?: config.values.getIntList("notificationFilter", listOf())?.mapNotNull { it.asNotificationTypeOrNull() }; return mNotificationFilter ?: listOf() }
        set(value) { config.set("notificationFilter", value.map { it.id }); mNotificationFilter = value }
}
