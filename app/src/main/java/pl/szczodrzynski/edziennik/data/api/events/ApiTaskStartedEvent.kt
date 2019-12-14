/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

class ApiTaskStartedEvent(val profileId: Int, val profile: Profile? = null)
