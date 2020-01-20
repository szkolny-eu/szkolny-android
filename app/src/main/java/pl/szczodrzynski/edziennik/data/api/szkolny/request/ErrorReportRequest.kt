/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-31.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.request

data class ErrorReportRequest(
        override val deviceId: String,
        override val device: Device? = null,

        val appVersion: String,
        val errors: List<Error>
) : ApiRequest(deviceId, device) {
    data class Error(
            val id: Long,
            val tag: String,
            val errorCode: Int,
            val errorText: String?,
            val errorReason: String?,
            val stackTrace: String?,
            val request: String?,
            val response: String?,
            val apiResponse: String?,
            val isCritical: Boolean
    )
}
