/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-31.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.request

data class ErrorReportRequest(
        val deviceId: String,
        val device: Device? = null,

        val appVersion: String,
        val errors: List<Error>
) {
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
