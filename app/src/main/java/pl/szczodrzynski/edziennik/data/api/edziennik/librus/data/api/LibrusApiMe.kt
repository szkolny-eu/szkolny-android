/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-3.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_ME
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.ext.*

class LibrusApiMe(override val data: DataLibrus,
                  override val lastSync: Long?,
                  val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
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
            data.profile?.accountName =
                    if (isParent)
                        buildFullName(account?.getString("FirstName"), account?.getString("LastName"))
                    else null

            data.profile?.studentNameLong =
                    buildFullName(user?.getString("FirstName"), user?.getString("LastName"))

            data.setSyncNext(ENDPOINT_LIBRUS_API_ME, 2* DAY)
            onSuccess(ENDPOINT_LIBRUS_API_ME)
        }
    }
}
