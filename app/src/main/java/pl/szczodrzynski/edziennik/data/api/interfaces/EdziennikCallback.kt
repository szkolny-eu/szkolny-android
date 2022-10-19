/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.data.api.interfaces

import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.models.Feature

/**
 * A callback passed only to an e-register class.
 * All [Feature]s and [LoginMethod]s receive this callback,
 * but may only use [EndpointCallback]'s methods.
 */
interface EdziennikCallback : EndpointCallback {
    fun onCompleted()
    fun onRequiresUserAction(event: UserActionRequiredEvent)
}
