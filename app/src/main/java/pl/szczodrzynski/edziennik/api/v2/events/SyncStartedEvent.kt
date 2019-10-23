/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.api.v2.events

import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

class SyncStartedEvent(val profileId: Int, val profile: Profile? = null)