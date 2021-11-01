/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import im.wangchao.mhttp.Response
import okhttp3.RequestBody
import okio.Buffer
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApiException
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ApiResponse
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun RequestBody.bodyToString(): String {
    val buffer = Buffer()
    writeTo(buffer)
    return buffer.readUtf8()
}

fun Response.toErrorCode() = when (this.code()) {
    400 -> ERROR_REQUEST_HTTP_400
    401 -> ERROR_REQUEST_HTTP_401
    403 -> ERROR_REQUEST_HTTP_403
    404 -> ERROR_REQUEST_HTTP_404
    405 -> ERROR_REQUEST_HTTP_405
    410 -> ERROR_REQUEST_HTTP_410
    424 -> ERROR_REQUEST_HTTP_424
    500 -> ERROR_REQUEST_HTTP_500
    503 -> ERROR_REQUEST_HTTP_503
    else -> null
}

fun Throwable.toErrorCode() = when (this) {
    is UnknownHostException -> ERROR_REQUEST_FAILURE_HOSTNAME_NOT_FOUND
    is SSLException -> ERROR_REQUEST_FAILURE_SSL_ERROR
    is SocketTimeoutException -> ERROR_REQUEST_FAILURE_TIMEOUT
    is InterruptedIOException, is ConnectException -> ERROR_REQUEST_FAILURE_NO_INTERNET
    is SzkolnyApiException -> this.error?.toErrorCode()
    else -> null
}
fun ApiResponse.Error.toErrorCode() = when (this.code) {
    "PdoError" -> ERROR_API_PDO_ERROR
    "InvalidClient" -> ERROR_API_INVALID_CLIENT
    "InvalidArgument" -> ERROR_API_INVALID_ARGUMENT
    "InvalidSignature" -> ERROR_API_INVALID_SIGNATURE
    "MissingScopes" -> ERROR_API_MISSING_SCOPES
    "ResourceNotFound" -> ERROR_API_RESOURCE_NOT_FOUND
    "InternalServerError" -> ERROR_API_INTERNAL_SERVER_ERROR
    "PhpError" -> ERROR_API_PHP_E_ERROR
    "PhpWarning" -> ERROR_API_PHP_E_WARNING
    "PhpParse" -> ERROR_API_PHP_E_PARSE
    "PhpNotice" -> ERROR_API_PHP_E_NOTICE
    "PhpOther" -> ERROR_API_PHP_E_OTHER
    "ApiMaintenance" -> ERROR_API_MAINTENANCE
    "MissingArgument" -> ERROR_API_MISSING_ARGUMENT
    "MissingPayload" -> ERROR_API_PAYLOAD_EMPTY
    "InvalidAction" -> ERROR_API_INVALID_ACTION
    "VersionNotFound" -> ERROR_API_UPDATE_NOT_FOUND
    "InvalidDeviceIdUserCode" -> ERROR_API_INVALID_DEVICEID_USERCODE
    "InvalidPairToken" -> ERROR_API_INVALID_PAIRTOKEN
    "InvalidBrowserId" -> ERROR_API_INVALID_BROWSERID
    "InvalidDeviceId" -> ERROR_API_INVALID_DEVICEID
    "InvalidDeviceIdBrowserId" -> ERROR_API_INVALID_DEVICEID_BROWSERID
    "HelpCategoryNotFound" -> ERROR_API_HELP_CATEGORY_NOT_FOUND
    else -> ERROR_API_EXCEPTION
}
fun Throwable.toApiError(tag: String) = ApiError.fromThrowable(tag, this)
