/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.data.api.models

import android.content.Context
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.ERROR_API_EXCEPTION
import pl.szczodrzynski.edziennik.data.api.ERROR_EXCEPTION
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApiException
import pl.szczodrzynski.edziennik.data.api.szkolny.request.ErrorReportRequest
import pl.szczodrzynski.edziennik.ext.stackTraceString
import pl.szczodrzynski.edziennik.ext.toErrorCode

class ApiError(val tag: String, var errorCode: Int) {
    companion object {
        fun fromThrowable(tag: String, throwable: Throwable) =
                ApiError(tag, throwable.toErrorCode() ?: ERROR_EXCEPTION)
                        .withThrowable(throwable)
    }

    val id = System.currentTimeMillis()
    var profileId: Int? = null
    var throwable: Throwable? = null
    var apiResponse: String? = null
    var request: Request? = null
    var response: Response? = null
    var isCritical = true

    fun withThrowable(throwable: Throwable?): ApiError {
        this.throwable = throwable
        return this
    }
    fun withApiResponse(apiResponse: String?): ApiError {
        this.apiResponse = apiResponse
        return this
    }
    fun withApiResponse(apiResponse: JsonObject?): ApiError {
        this.apiResponse = apiResponse?.toString()
        return this
    }
    fun withRequest(request: Request?): ApiError {
        this.request = request
        return this
    }
    fun withResponse(response: Response?): ApiError {
        this.response = response
        this.request = response?.request()
        return this
    }

    fun setCritical(isCritical: Boolean): ApiError {
        this.isCritical = isCritical
        return this
    }

    fun getStringText(context: Context): String {
        return context.resources.getIdentifier("error_${errorCode}", "string", context.packageName).let {
            if (it != 0)
                context.getString(it)
            else
                "?"
        }
    }

    fun getStringReason(context: Context): String {
        if (errorCode == ERROR_API_EXCEPTION && throwable is SzkolnyApiException)
            return throwable?.message.toString()
        return context.resources.getIdentifier("error_${errorCode}_reason", "string", context.packageName).let {
            if (it != 0)
                context.getString(it)
            else
                context.getString(R.string.error_unknown_format, errorCode, tag)
        }
    }

    override fun toString(): String {
        return "ApiError(tag='$tag', errorCode=$errorCode, profileId=$profileId, throwable=$throwable, apiResponse=$apiResponse, request=$request, response=$response, isCritical=$isCritical)"
    }

    fun toReportableError(context: Context): ErrorReportRequest.Error {
        val requestString = request?.let {
            it.method() + " " + it.url() + "\n" + it.headers() + "\n\n" + (it.jsonBody()?.toString() ?: "") + (it.textBody() ?: "")
        }
        val responseString = response?.let {
            if (it.parserErrorBody == null) {
                try {
                    it.parserErrorBody = it.raw().body()?.string()
                } catch (e: Exception) {
                    it.parserErrorBody = e.stackTraceString
                }
            }
            "HTTP "+it.code()+" "+it.message()+"\n" + it.headers() + "\n\n" + it.parserErrorBody
        }
        return ErrorReportRequest.Error(
                id = id,
                tag = tag,
                errorCode = errorCode,
                errorText = getStringText(context),
                errorReason = getStringReason(context),
                stackTrace = throwable?.stackTraceString,
                request = requestString,
                response = responseString,
                apiResponse = apiResponse ?: response?.parserErrorBody,
                isCritical = isCritical
        )
    }

}
