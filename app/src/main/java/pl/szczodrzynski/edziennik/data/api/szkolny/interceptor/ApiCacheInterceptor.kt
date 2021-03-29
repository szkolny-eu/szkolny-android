/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-29.
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.interceptor

import okhttp3.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ApiResponse
import pl.szczodrzynski.edziennik.md5

class ApiCacheInterceptor(val app: App) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (Signing.appCertificate.md5() == app.config.apiInvalidCert) {
            val response = ApiResponse<Unit>(
                success = false,
                errors = listOf(ApiResponse.Error("InvalidSignature", ""))
            )

            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .addHeader("Content-Type", "application/json")
                .body(ResponseBody.create(
                    MediaType.parse("application/json"),
                    app.gson.toJson(response)
                ))
                .build()
        }

        return chain.proceed(chain.request())
    }
}
