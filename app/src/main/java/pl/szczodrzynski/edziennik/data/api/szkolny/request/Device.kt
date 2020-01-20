/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-20.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.request

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
