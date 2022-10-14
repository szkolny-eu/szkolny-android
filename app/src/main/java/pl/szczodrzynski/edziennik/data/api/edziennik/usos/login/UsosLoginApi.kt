/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.login

import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_OAUTH_LOGIN_REQUEST
import pl.szczodrzynski.edziennik.data.api.USOS_API_OAUTH_REDIRECT_URL
import pl.szczodrzynski.edziennik.data.api.USOS_API_SCOPES
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.fromQueryString
import pl.szczodrzynski.edziennik.ext.toBundle
import pl.szczodrzynski.edziennik.ext.toQueryString
import pl.szczodrzynski.edziennik.utils.Utils.d

class UsosLoginApi(val data: DataUsos, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "UsosLoginApi"
    }

    init { run {
        if (data.isApiLoginValid()) {
            onSuccess()
        } else if (data.oauthLoginResponse != null) {
            login()
        } else {
            authorize()
        }
    }}

    private fun authorize() {
        val api = UsosApi(data, null)

        api.apiRequest<String>(
            tag = TAG,
            service = "oauth/request_token",
            params = mapOf(
                "oauth_callback" to USOS_API_OAUTH_REDIRECT_URL,
                "scopes" to USOS_API_SCOPES,
            ),
            responseType = UsosApi.ResponseType.PLAIN,
        ) {
            val response = it.fromQueryString()
            data.oauthTokenKey = response["oauth_token"]
            data.oauthTokenSecret = response["oauth_token_secret"]

            val authUrl = "${data.instanceUrl}services/oauth/authorize"
            val authParams = mapOf(
                "interactivity" to "confirm_user",
                "oauth_token" to (data.oauthTokenKey ?: ""),
            )
            val params = Bundle(
                "authorizeUrl" to "$authUrl?${authParams.toQueryString()}",
                "redirectUrl" to USOS_API_OAUTH_REDIRECT_URL,
                "responseStoreKey" to "oauthLoginResponse",
                "extras" to data.loginStore.data.toBundle(),
            )
            data.error(ApiError(TAG, ERROR_USOS_OAUTH_LOGIN_REQUEST).withParams(params))
        }
    }

    private fun login() {
        d(TAG, "Login to ${data.schoolId} with ${data.oauthLoginResponse} (${data.oauthTokenSecret})")
    }
}
