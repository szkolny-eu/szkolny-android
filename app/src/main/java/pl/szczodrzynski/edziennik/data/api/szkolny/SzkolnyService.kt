/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny

import pl.szczodrzynski.edziennik.data.api.szkolny.request.*
import pl.szczodrzynski.edziennik.data.api.szkolny.response.*
import pl.szczodrzynski.edziennik.ui.modules.login.LoginInfo
import retrofit2.Call
import retrofit2.http.*

interface SzkolnyService {

    @POST("appSync")
    fun serverSync(@Body request: ServerSyncRequest): Call<ApiResponse<ServerSyncResponse>>

    @POST("share")
    fun shareEvent(@Body request: EventShareRequest): Call<ApiResponse<Unit>>

    @POST("webPush")
    fun webPush(@Body request: WebPushRequest): Call<ApiResponse<WebPushResponse>>

    @POST("errorReport")
    fun errorReport(@Body request: ErrorReportRequest): Call<ApiResponse<Unit>>

    @POST("appUser")
    fun appUser(@Body request: AppUserRequest): Call<ApiResponse<Unit>>

    @GET("updates/app")
    fun updates(@Query("channel") channel: String = "release"): Call<ApiResponse<List<Update>>>

    @POST("feedbackMessage")
    fun feedbackMessage(@Body request: FeedbackMessageRequest): Call<ApiResponse<FeedbackMessageResponse>>

    @GET("appLogin/platforms/{registerName}")
    fun appLoginPlatforms(@Path("registerName") registerName: String): Call<ApiResponse<List<LoginInfo.Platform>>>

    @GET("firebase/token/{registerName}")
    fun firebaseToken(@Path("registerName") registerName: String): Call<ApiResponse<String>>
}
