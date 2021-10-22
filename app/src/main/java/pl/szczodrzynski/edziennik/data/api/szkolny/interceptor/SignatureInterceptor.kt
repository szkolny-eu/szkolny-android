/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.ext.bodyToString
import pl.szczodrzynski.edziennik.ext.currentTimeUnix
import pl.szczodrzynski.edziennik.ext.hmacSHA1
import pl.szczodrzynski.edziennik.ext.md5

class SignatureInterceptor(val app: App) : Interceptor {
    companion object {
        private const val API_KEY = "szkolny_android_42a66f0842fc7da4e37c66732acf705a"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val timestamp = currentTimeUnix()
        val body = request.body()?.bodyToString() ?: ""
        val url = request.url().toString()

        return chain.proceed(
                request.newBuilder()
                        .header("X-ApiKey", API_KEY)
                        .header("X-AppVersion", BuildConfig.VERSION_CODE.toString())
                        .header("X-Timestamp", timestamp.toString())
                        .header("X-Signature", sign(timestamp, body, url))
                        .header("X-AppBuild", BuildConfig.BUILD_TYPE)
                        .header("X-AppFlavor", BuildConfig.FLAVOR)
                        .build())
    }

    private fun sign(timestamp: Long, body: String, url: String): String {
        val content = timestamp.toString().md5() + body.md5() + url.md5()
        val password = Signing.appPassword + BuildConfig.VERSION_CODE.toString() + Signing.appCertificate

        return content.hmacSHA1(password)
    }
}
