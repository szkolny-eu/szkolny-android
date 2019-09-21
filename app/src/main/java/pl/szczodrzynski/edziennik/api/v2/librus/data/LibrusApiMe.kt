/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.api.v2.models.Data
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile

class LibrusApiMe(override val data: DataLibrus,
                  val onSuccess: () -> Unit) : LibrusApi(data) {
    init {
        apiRequest("Me") { json ->
            val me = json?.getJsonObject("Me")
            val account = me?.getJsonObject("Account")
            val user = me?.getJsonObject("User")

            data.isPremium = account?.getBoolean("isPremium") == true || account?.getBoolean("isPremiumDemo") == true

            val isParent = account?.getInt("GroupId") == 5
            data.profile?.accountNameLong =
                    if (isParent)
                        buildFullName(account?.getString("FirstName"), account?.getString("LastName"))
                    else null

            data.profile?.studentNameLong =
                    buildFullName(user?.getString("FirstName"), user?.getString("LastName"))

            onSuccess()
        }
    }
}