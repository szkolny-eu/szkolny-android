/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.Utils.d

class UsosLoginApi(val data: DataUsos, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "UsosLoginApi"
    }

    private val api = UsosApi(data, null)

    init {
        run {
            data.arguments?.getString("oauthLoginResponse")?.let {
                data.oauthLoginResponse = it
            }
            if (data.isApiLoginValid()) {
                onSuccess()
            } else if (data.oauthLoginResponse != null) {
                login()
            } else {
                authorize()
            }
        }
    }

    private fun authorize() {
        data.oauthTokenKey = null
        data.oauthTokenSecret = null
        api.apiRequest<String>(
            tag = TAG,
            service = "oauth/request_token",
            params = mapOf(
                "oauth_callback" to USOS_API_OAUTH_REDIRECT_URL,
                "scopes" to USOS_API_SCOPES,
            ),
            responseType = UsosApi.ResponseType.PLAIN,
        ) { text, _ ->
            val authorizeData = text.fromQueryString()
            data.oauthTokenKey = authorizeData["oauth_token"]
            data.oauthTokenSecret = authorizeData["oauth_token_secret"]
            data.oauthTokenIsUser = false

            val authUrl = "${data.instanceUrl}services/oauth/authorize"
            val authParams = mapOf(
                "interactivity" to "confirm_user",
                "oauth_token" to (data.oauthTokenKey ?: ""),
            )
            data.requireUserAction(
                type = UserActionRequiredEvent.Type.OAUTH,
                params = Bundle(
                    "authorizeUrl" to "$authUrl?${authParams.toQueryString()}",
                    "redirectUrl" to USOS_API_OAUTH_REDIRECT_URL,
                    "responseStoreKey" to "oauthLoginResponse",
                    "extras" to data.loginStore.data.toBundle(),
                ),
                errorText = R.string.notification_user_action_required_oauth_usos,
            )
        }
    }

    private fun login() {
        d(TAG, "Login to ${data.schoolId} with ${data.oauthLoginResponse}")

        val authorizeResponse = data.oauthLoginResponse?.fromQueryString()
            ?: return // checked in init {}
        if (authorizeResponse["oauth_token"] != data.oauthTokenKey) {
            // got different token
            data.error(ApiError(TAG, ERROR_USOS_OAUTH_GOT_DIFFERENT_TOKEN)
                .withApiResponse(data.oauthLoginResponse))
            return
        }
        val verifier = authorizeResponse["oauth_verifier"]
        if (verifier.isNullOrBlank()) {
            data.error(ApiError(TAG, ERROR_USOS_OAUTH_INCOMPLETE_RESPONSE)
                .withApiResponse(data.oauthLoginResponse))
            return
        }

        api.apiRequest<String>(
            tag = TAG,
            service = "oauth/access_token",
            params = mapOf(
                "oauth_verifier" to verifier,
            ),
            responseType = UsosApi.ResponseType.PLAIN,
        ) { text, response ->
            val accessData = text.fromQueryString()
            data.oauthTokenKey = accessData["oauth_token"]
            data.oauthTokenSecret = accessData["oauth_token_secret"]
            data.oauthTokenIsUser = data.oauthTokenKey != null && data.oauthTokenSecret != null
            data.loginStore.removeLoginData("oauthLoginResponse")

            if (!data.oauthTokenIsUser)
                data.error(ApiError(TAG, ERROR_USOS_OAUTH_INCOMPLETE_RESPONSE)
                    .withApiResponse(text)
                    .withResponse(response))
            else
                onSuccess()
        }
    }
}
