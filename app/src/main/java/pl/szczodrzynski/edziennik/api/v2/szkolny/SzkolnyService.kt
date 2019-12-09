/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.api.v2.szkolny

import pl.szczodrzynski.edziennik.api.v2.szkolny.request.ServerSyncRequest
import pl.szczodrzynski.edziennik.api.v2.szkolny.response.ApiResponse
import pl.szczodrzynski.edziennik.api.v2.szkolny.response.ServerSyncResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface SzkolnyService {

    @POST("appSync")
    fun serverSync(@Body request: ServerSyncRequest): Call<ApiResponse<ServerSyncResponse>>
}
