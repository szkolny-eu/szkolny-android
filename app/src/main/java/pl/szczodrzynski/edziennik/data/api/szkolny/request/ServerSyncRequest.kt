/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.request

data class ServerSyncRequest(

        val deviceId: String,
        val device: Device? = null,

        val userCodes: List<String>,
        val users: List<User>? = null,

        val notifications: List<Notification>? = null
) {
    data class Device(
            val osType: String,
            val osVersion: String,
            val hardware: String,
            val pushToken: String?,
            val appVersion: String,
            val appType: String,
            val appVersionCode: Int,
            val syncInterval: Int
    )

    data class User(
            val userCode: String,
            val studentName: String,
            val studentNameShort: String,
            val loginType: Int,
            val teamCodes: List<String>
    )

    data class Notification(
            val profileName: String,
            val type: Int,
            val text: String
    )
}