/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.api.v2.interfaces

import pl.szczodrzynski.edziennik.api.v2.models.Endpoint
import pl.szczodrzynski.edziennik.api.v2.models.LoginMethod

/**
 * A callback passed only to an e-register class.
 * All [Endpoint]s and [LoginMethod]s receive this callback,
 * but may only use [EndpointCallback]'s methods.
 */
interface EdziennikCallback : EndpointCallback {
    fun onCompleted()
}