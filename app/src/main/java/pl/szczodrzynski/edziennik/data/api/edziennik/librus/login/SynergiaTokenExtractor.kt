package pl.szczodrzynski.edziennik.data.api.edziennik.librus.login

import com.google.gson.JsonObject
import im.wangchao.mhttp.Response
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusPortal
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.ext.*
import timber.log.Timber

class SynergiaTokenExtractor(override val data: DataLibrus, val onSuccess: () -> Unit) : LibrusPortal(data) {
    companion object {
        private const val TAG = "SynergiaTokenExtractor"
    }

    init { run {
        if (data.loginStore.mode != LoginMode.LIBRUS_EMAIL) {
            data.error(ApiError(TAG, ERROR_INVALID_LOGIN_MODE))
            return@run
        }
        if (data.profile == null) {
            data.error(ApiError(TAG, ERROR_PROFILE_MISSING))
            return@run
        }

        if (data.apiTokenExpiryTime-30 > currentTimeUnix() && data.apiAccessToken.isNotNullNorEmpty()) {
            onSuccess()
        }
        else {
            if (!synergiaAccount()) {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    /**
     * Get an Api token from the Portal account, using Portal API.
     * If necessary, refreshes the token.
     */
    private fun synergiaAccount(): Boolean {

        val accountLogin = data.apiLogin ?: return false
        data.portalAccessToken ?: return false

        Timber.d("Request: Librus/SynergiaTokenExtractor - ${if (data.fakeLogin) FAKE_LIBRUS_ACCOUNT else LIBRUS_ACCOUNT_URL}$accountLogin")

        val onSuccess = { json: JsonObject, response: Response? ->
            // synergiaAccount is executed when a synergia token needs a refresh
            val accountId = json.getInt("id")
            val accountToken = json.getString("accessToken")
            if (accountId == null || accountToken == null) {
                data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_PORTAL_SYNERGIA_TOKEN_MISSING)
                        .withResponse(response)
                        .withApiResponse(json))
            }
            else {
                data.apiAccessToken = accountToken
                data.apiTokenExpiryTime = response.getUnixDate() + 6 * 60 * 60

                onSuccess()
            }
        }

        portalGet(TAG, (if (data.fakeLogin) FAKE_LIBRUS_ACCOUNT else LIBRUS_ACCOUNT_URL)+accountLogin, onSuccess = onSuccess)
        return true
    }
}
