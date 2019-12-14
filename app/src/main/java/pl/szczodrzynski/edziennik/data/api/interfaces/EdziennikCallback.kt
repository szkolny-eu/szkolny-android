/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.data.api.interfaces

import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.api.models.LoginMethod

/**
 * A callback passed only to an e-register class.
 * All [Feature]s and [LoginMethod]s receive this callback,
 * but may only use [EndpointCallback]'s methods.
 */
interface EdziennikCallback : EndpointCallback {
    fun onCompleted()
}
