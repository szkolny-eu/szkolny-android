/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.api.v2.models

import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import pl.szczodrzynski.edziennik.data.api.AppError

class ApiError(val tag: String, val errorCode: Int) {
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

    fun toAppError(): AppError {
        return AppError(
                tag,
                -1,
                errorCode, response, throwable, apiResponse
        )
    }

    override fun toString(): String {
        return "ApiError(tag='$tag', errorCode=$errorCode, profileId=$profileId, throwable=$throwable, apiResponse=$apiResponse, request=$request, response=$response, isCritical=$isCritical)"
    }


}