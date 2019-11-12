/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_USERS
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher

class LibrusApiUsers(override val data: DataLibrus,
                        val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiUsers"
    }

    init {
        apiGet(TAG, "Users") { json ->
            val users = json.getJsonArray("Users").asJsonObjectList()

            users?.forEach { user ->
                val id = user.getLong("Id") ?: return@forEach
                val firstName = user.getString("FirstName")?.fixName() ?: ""
                val lastName = user.getString("LastName")?.fixName() ?: ""

                data.teacherList.put(id, Teacher(profileId, id, firstName, lastName))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_USERS, 4*DAY)
            onSuccess()
        }
    }
}
