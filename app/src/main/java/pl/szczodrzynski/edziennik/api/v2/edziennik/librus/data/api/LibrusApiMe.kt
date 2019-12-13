/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-3.
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.ENDPOINT_LIBRUS_API_ME
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.data.LibrusApi

class LibrusApiMe(override val data: DataLibrus,
                  val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiMe"
    }

    init {
        apiGet(TAG, "Me") { json ->
            val me = json.getJsonObject("Me")
            val account = me?.getJsonObject("Account")
            val user = me?.getJsonObject("User")

            data.isPremium = account?.getBoolean("IsPremium") == true || account?.getBoolean("IsPremiumDemo") == true

            val isParent = account?.getInt("GroupId") == 5
            data.profile?.accountNameLong =
                    if (isParent)
                        buildFullName(account?.getString("FirstName"), account?.getString("LastName"))
                    else null

            data.profile?.studentNameLong =
                    buildFullName(user?.getString("FirstName"), user?.getString("LastName"))

            data.setSyncNext(ENDPOINT_LIBRUS_API_ME, 2*DAY)
            onSuccess()
        }
    }
}
