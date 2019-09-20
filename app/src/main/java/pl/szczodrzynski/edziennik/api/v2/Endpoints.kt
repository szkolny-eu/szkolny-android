/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2

import android.util.Log
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApiGrades
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusSynergiaGrades
import pl.szczodrzynski.edziennik.api.v2.models.Endpoint

const val ENDPOINT_LIBRUS_API_ME = 0
const val ENDPOINT_LIBRUS_API_GRADES = 0
const val ENDPOINT_LIBRUS_SYNERGIA_GRADES = 0

val librusEndpoints = listOf(
        Endpoint(LOGIN_TYPE_LIBRUS, 1, listOf(), LibrusSynergiaGrades::class.java) { _, _ -> LOGIN_METHOD_LIBRUS_SYNERGIA },
        Endpoint(LOGIN_TYPE_LIBRUS, 1, listOf(), LibrusApiGrades::class.java) { _, _ -> LOGIN_METHOD_LIBRUS_API }
)

/*
    SYNC:

    look up all endpoints for the given API and given features

    load "next sync timers" for every endpoint

    exclude every endpoint which does not need to sync now

    check all needed login methods
        create a login method list, using methods' dependencies as well
        use all login methods, saving completed logins to data store

    instantiate all endpoint classes and sync them (writing to data store, returns onSuccess or error Callback)

 */
