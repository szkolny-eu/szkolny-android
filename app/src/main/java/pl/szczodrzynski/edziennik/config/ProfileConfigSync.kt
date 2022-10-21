/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-21.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.data.db.enums.NotificationType

@Suppress("RemoveExplicitTypeArguments")
class ProfileConfigSync(base: ProfileConfig) {

    var notificationFilter by base.config<Set<NotificationType>> { setOf() }
}
