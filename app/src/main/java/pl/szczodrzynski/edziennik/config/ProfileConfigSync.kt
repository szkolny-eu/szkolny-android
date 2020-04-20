/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-21.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.set

class ProfileConfigSync(private val config: ProfileConfig) {
    private var mNotificationFilter: List<Int>? = null
    var notificationFilter: List<Int>
        get() { mNotificationFilter = mNotificationFilter ?: config.values.get("notificationFilter", listOf()); return mNotificationFilter ?: listOf() }
        set(value) { config.set("notificationFilter", value); mNotificationFilter = value }
}
