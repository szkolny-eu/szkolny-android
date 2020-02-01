/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6. 
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login

import android.os.Build
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import io.github.wulkanowy.signer.android.getPrivateKeyFromCert
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api.VulcanApiUpdateSemester
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.util.*
import java.util.regex.Pattern

class VulcanLoginApi(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "VulcanLoginApi"
    }

    init { run {
        if (data.profile != null && data.isApiLoginValid()) {
            onSuccess()
        }
        else {
            // < v4.0 - PFX to Private Key migration
            if (data.apiCertificatePfx.isNotNullNorEmpty()) {
                try {
                    data.apiCertificatePrivate = getPrivateKeyFromCert(
                            if (data.apiToken?.get(0) == 'F') VULCAN_API_PASSWORD_FAKELOG else VULCAN_API_PASSWORD,
                            data.apiCertificatePfx ?: ""
                    )
                    data.loginStore.removeLoginData("certificatePfx")
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    onSuccess()
                    return@run
                }
            }

            if (data.apiCertificateKey.isNotNullNorEmpty()
                    && data.apiCertificatePrivate.isNotNullNorEmpty()
                    && data.symbol.isNotNullNorEmpty()) {
                // (see data.isApiLoginValid())
                // the semester end date is over
                VulcanApiUpdateSemester(data, onSuccess)
                return@run
            }

            if (data.symbol.isNotNullNorEmpty() && data.apiToken.isNotNullNorEmpty() && data.apiPin.isNotNullNorEmpty()) {
                loginWithToken()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    private fun loginWithToken() {
        d(TAG, "Request: Vulcan/Login/Api - ${data.apiUrl}/$VULCAN_API_ENDPOINT_CERTIFICATE")

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null) {
                    if (response?.code() == HTTP_BAD_REQUEST) {
                        data.error(TAG, ERROR_LOGIN_VULCAN_INVALID_SYMBOL, response)
                        return
                    }
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                var tokenStatus = json.getString("TokenStatus")
                if (tokenStatus == "Null" || tokenStatus == "CertGenerated")
                    tokenStatus = null
                val error = tokenStatus ?: json.getString("Message")
                error?.let { code ->
                    when (code) {
                        "TokenNotFound" -> ERROR_LOGIN_VULCAN_INVALID_TOKEN
                        "TokenDead" -> ERROR_LOGIN_VULCAN_EXPIRED_TOKEN
                        "WrongPIN" -> {
                            Pattern.compile("Liczba pozostałych prób: ([0-9])", Pattern.DOTALL).matcher(tokenStatus).let { matcher ->
                                if (matcher.matches())
                                    ERROR_LOGIN_VULCAN_INVALID_PIN + 1 + matcher.group(1).toInt()
                                else
                                    ERROR_LOGIN_VULCAN_INVALID_PIN
                            }
                        }
                        "Broken" -> ERROR_LOGIN_VULCAN_INVALID_PIN_0_REMAINING
                        "OnlyKindergarten" -> ERROR_LOGIN_VULCAN_ONLY_KINDERGARTEN
                        "NoPupils" -> ERROR_LOGIN_VULCAN_NO_PUPILS
                        else -> ERROR_LOGIN_VULCAN_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(TAG, errorCode)
                                .withApiResponse(json)
                                .withResponse(response))
                        return
                    }
                }

                val cert = json.getJsonObject("TokenCert")
                if (cert == null) {
                    data.error(ApiError(TAG, ERROR_LOGIN_VULCAN_OTHER)
                            .withApiResponse(json)
                            .withResponse(response))
                    return
                }

                data.apiCertificateKey = cert.getString("CertyfikatKlucz")
                data.apiToken = data.apiToken?.substring(0, 3)
                data.apiCertificatePrivate = getPrivateKeyFromCert(
                        if (data.apiToken?.get(0) == 'F') VULCAN_API_PASSWORD_FAKELOG else VULCAN_API_PASSWORD,
                        cert.getString("CertyfikatPfx") ?: ""
                )
                data.loginStore.removeLoginData("certificatePfx")
                data.loginStore.removeLoginData("devicePin")
                onSuccess()
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("${data.apiUrl}/$VULCAN_API_ENDPOINT_CERTIFICATE")
                .userAgent(VULCAN_API_USER_AGENT)
                .addHeader("RequestMobileType", "RegisterDevice")
                .addParameter("PIN", data.apiPin)
                .addParameter("TokenKey", data.apiToken)
                .addParameter("DeviceId", UUID.randomUUID().toString())
                .addParameter("DeviceName", VULCAN_API_DEVICE_NAME)
                .addParameter("DeviceNameUser", "")
                .addParameter("DeviceDescription", "")
                .addParameter("DeviceSystemType", "Android")
                .addParameter("DeviceSystemVersion", Build.VERSION.RELEASE)
                .addParameter("RemoteMobileTimeKey", currentTimeUnix())
                .addParameter("TimeKey", currentTimeUnix() - 1)
                .addParameter("RequestId", UUID.randomUUID().toString())
                .addParameter("AppVersion", VULCAN_API_APP_VERSION)
                .addParameter("RemoteMobileAppVersion", VULCAN_API_APP_VERSION)
                .addParameter("RemoteMobileAppName", VULCAN_API_APP_NAME)
                .postJson()
                .allowErrorCode(HTTP_BAD_REQUEST)
                .callback(callback)
                .build()
                .enqueue()
    }
}
