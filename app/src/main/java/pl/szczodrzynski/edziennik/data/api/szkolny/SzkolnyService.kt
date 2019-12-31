/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny

import pl.szczodrzynski.edziennik.data.api.szkolny.request.ErrorReportRequest
import pl.szczodrzynski.edziennik.data.api.szkolny.request.EventShareRequest
import pl.szczodrzynski.edziennik.data.api.szkolny.request.ServerSyncRequest
import pl.szczodrzynski.edziennik.data.api.szkolny.request.WebPushRequest
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ApiResponse
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ServerSyncResponse
import pl.szczodrzynski.edziennik.data.api.szkolny.response.WebPushResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface SzkolnyService {

    @POST("appSync")
    fun serverSync(@Body request: ServerSyncRequest): Call<ApiResponse<ServerSyncResponse>>

    @POST("share")
    fun shareEvent(@Body request: EventShareRequest): Call<ApiResponse<Nothing>>

    @POST("webPush")
    fun webPush(@Body request: WebPushRequest): Call<ApiResponse<WebPushResponse>>

    @POST("errorReport")
    fun errorReport(@Body request: ErrorReportRequest): Call<ApiResponse<Nothing>>
}
