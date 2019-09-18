package pl.szczodrzynski.edziennik.api.v2.librus.login

import android.util.Pair
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.JsonCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.AppError
import pl.szczodrzynski.edziennik.api.AppError.*
import pl.szczodrzynski.edziennik.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils.c
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.util.ArrayList
import java.util.regex.Pattern

class LoginLibrus(val app: App, val loginStore: LoginStore, val callback: ProgressCallback, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "librus.LoginLibrus"
    }

    init {
        // ustawiamy tokeny, generujemy itp
        // nic nie robimy z dostępem do api.librus.pl
        // to będzie później
        val accessToken = loginStore.getLoginData("accessToken", null)
        val refreshToken = loginStore.getLoginData("refreshToken", null)
        val tokenExpiryTime = loginStore.getLoginData("tokenExpiryTime", 0L)

        // succeed having a non-expired access token and a refresh token
        if (tokenExpiryTime-30 > System.currentTimeMillis() / 1000 && refreshToken != null && accessToken != null) {
            onSuccess()
        }
        else if (refreshToken != null) {
            app.cookieJar.clearForDomain("portal.librus.pl")
            accessToken(null, refreshToken)
        }
        else {
            app.cookieJar.clearForDomain("portal.librus.pl")
            authorize(LIBRUS_AUTHORIZE_URL)
        }
    }

    private fun authorize(url: String?) {
        callback.onActionStarted(R.string.sync_action_authorizing)
        Request.builder()
                .url(url)
                .userAgent(LIBRUS_USER_AGENT)
                .withClient(app.httpLazy)
                .callback(object : TextCallbackHandler() {
                    override fun onSuccess(data: String, response: Response) {
                        //d("headers "+response.headers().toString());
                        val location = response.headers().get("Location")
                        if (location != null) {
                            val authMatcher = Pattern.compile("http://localhost/bar\\?code=([A-z0-9]+?)$", Pattern.DOTALL or Pattern.MULTILINE).matcher(location)
                            if (authMatcher.find()) {
                                accessToken(authMatcher.group(1), null)
                            } else {
                                //callback.onError(activityContext, Edziennik.CODE_OTHER, "Auth code not found: "+location);
                                authorize(location)
                            }
                        } else {
                            val csrfMatcher = Pattern.compile("name=\"csrf-token\" content=\"([A-z0-9=+/\\-_]+?)\"", Pattern.DOTALL).matcher(data)
                            if (csrfMatcher.find()) {
                                login(csrfMatcher.group(1))
                            } else {
                                callback.onError(null, AppError(TAG, 463, CODE_OTHER, "CSRF token not found.", response, data))
                            }
                        }
                    }

                    override fun onFailure(response: Response, throwable: Throwable) {
                        callback.onError(null, AppError(TAG, 207, CODE_OTHER, response, throwable))
                    }
                })
                .build()
                .enqueue()
    }

    private fun login(csrfToken: String) {
        callback.onActionStarted(R.string.sync_action_logging_in)
        val email = loginStore.getLoginData("email", "")
        val password = loginStore.getLoginData("password", "")
        Request.builder()
                .url(LIBRUS_LOGIN_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addParameter("email", email)
                .addParameter("password", password)
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaTypeUtils.APPLICATION_JSON)
                .post()
                .callback(object : JsonCallbackHandler() {
                    override fun onSuccess(data: JsonObject?, response: Response) {
                        if (data == null) {
                            if (response.parserErrorBody != null && response.parserErrorBody.contains("wciąż nieaktywne")) {
                                callback.onError(null, AppError(TAG, 487, CODE_LIBRUS_NOT_ACTIVATED, response))
                            }
                            callback.onError(null, AppError(TAG, 489, CODE_MAINTENANCE, response))
                            return
                        }
                        if (data.get("errors") != null) {
                            callback.onError(null, AppError(TAG, 490, CODE_OTHER, data.get("errors").asJsonArray.get(0).asString, response, data))
                            return
                        }
                        authorize(data.getString("redirect") ?: LIBRUS_AUTHORIZE_URL)
                    }

                    override fun onFailure(response: Response, throwable: Throwable) {
                        if (response.code() == 403 || response.code() == 401) {
                            callback.onError(null, AppError(TAG, 248, CODE_INVALID_LOGIN, response, throwable))
                            return
                        }
                        callback.onError(null, AppError(TAG, 251, CODE_OTHER, response, throwable))
                    }
                })
                .build()
                .enqueue()
    }

    private var refreshTokenFailed = false
    private fun accessToken(code: String?, refreshToken: String?) {
        callback.onActionStarted(R.string.sync_action_getting_token)
        val params = ArrayList<Pair<String, Any>>()
        params.add(Pair("client_id", LIBRUS_CLIENT_ID))
        if (code != null) {
            params.add(Pair("grant_type", "authorization_code"))
            params.add(Pair("code", code))
            params.add(Pair("redirect_uri", LIBRUS_REDIRECT_URL))
        } else if (refreshToken != null) {
            params.add(Pair("grant_type", "refresh_token"))
            params.add(Pair("refresh_token", refreshToken))
        }
        Request.builder()
                .url(LIBRUS_TOKEN_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addParams(params)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .post()
                .callback(object : JsonCallbackHandler() {
                    override fun onSuccess(data: JsonObject?, response: Response) {
                        if (data == null) {
                            callback.onError(null, AppError(TAG, 539, CODE_MAINTENANCE, response))
                            return
                        }
                        if (data.get("error") != null) {
                            val hint = data.getString("hint")
                            if (!refreshTokenFailed && refreshToken != null && (hint == "Token has been revoked" || hint == "Token has expired")) {
                                c(TAG, "refreshing the token failed. Trying to log in again.")
                                refreshTokenFailed = true
                                authorize(LIBRUS_AUTHORIZE_URL)
                                return
                            }
                            val errorText = data.getString("error") + " " + (data.getString("message") ?: "") + " " + (hint ?: "")
                            callback.onError(null, AppError(TAG, 552, CODE_OTHER, errorText, response, data))
                            return
                        }
                        try {
                            loginStore.putLoginData("tokenType", data.getString("token_type"))
                            loginStore.putLoginData("accessToken", data.getString("access_token"))
                            loginStore.putLoginData("refreshToken", data.getString("refresh_token"))
                            loginStore.putLoginData("tokenExpiryTime", System.currentTimeMillis() / 1000 + (data.getInt("expires_in") ?: 86400))
                            onSuccess()
                        } catch (e: NullPointerException) {
                            callback.onError(null, AppError(TAG, 311, CODE_OTHER, response, e, data))
                        }

                    }

                    override fun onFailure(response: Response, throwable: Throwable) {
                        callback.onError(null, AppError(TAG, 317, CODE_OTHER, response, throwable))
                    }
                })
                .build()
                .enqueue()
    }
}