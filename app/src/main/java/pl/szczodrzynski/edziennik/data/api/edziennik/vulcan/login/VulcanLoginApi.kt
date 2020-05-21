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
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api.VulcanApiUpdateSemester
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.util.*
import java.util.regex.Pattern

class VulcanLoginApi(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "VulcanLoginApi"
    }

    init { run {
        if (data.studentSemesterNumber == 1 && data.semester1Id == 0)
            data.semester1Id = data.studentSemesterNumber
        if (data.studentSemesterNumber == 2 && data.semester2Id == 0)
            data.semester2Id = data.studentSemesterNumber

        copyFromLoginStore()

        if (data.profile != null && data.isApiLoginValid()) {
            onSuccess()
        }
        else {
            if (data.apiFingerprint[data.symbol].isNotNullNorEmpty()
                    && data.apiPrivateKey[data.symbol].isNotNullNorEmpty()
                    && data.symbol.isNotNullNorEmpty()) {
                // (see data.isApiLoginValid())
                // the semester end date is over
                VulcanApiUpdateSemester(data, null) { onSuccess() }
                return@run
            }

            if (data.symbol.isNotNullNorEmpty() && data.apiToken[data.symbol].isNotNullNorEmpty() && data.apiPin[data.symbol].isNotNullNorEmpty()) {
                loginWithToken()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    private fun copyFromLoginStore() {
        data.loginStore.data.apply {
            // < v4.0 - PFX to Private Key migration
            if (has("certificatePfx")) {
                try {
                    val privateKey = getPrivateKeyFromCert(
                            if (data.apiToken[data.symbol]?.get(0) == 'F') VULCAN_API_PASSWORD_FAKELOG else VULCAN_API_PASSWORD,
                            getString("certificatePfx") ?: ""
                    )
                    data.apiPrivateKey = mapOf(
                            data.symbol to privateKey
                    )
                    remove("certificatePfx")
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            // 4.0 - new login form - copy user input to profile
            if (has("symbol")) {
                data.symbol = getString("symbol")
                remove("symbol")
            }

            // 4.0 - before Vulcan Web impl - migrate from strings to Map of Symbol to String
            if (has("deviceSymbol")) {
                data.symbol = getString("deviceSymbol")
                remove("deviceSymbol")
            }
            if (has("certificateKey")) {
                data.apiFingerprint = data.apiFingerprint.toMutableMap().also {
                    it[data.symbol] = getString("certificateKey")
                }
                remove("certificateKey")
            }
            if (has("certificatePrivate")) {
                data.apiPrivateKey = data.apiPrivateKey.toMutableMap().also {
                    it[data.symbol] = getString("certificatePrivate")
                }
                remove("certificatePrivate")
            }

            // map form inputs to the symbol
            if (has("deviceToken")) {
                data.apiToken = data.apiToken.toMutableMap().also {
                    it[data.symbol] = getString("deviceToken")
                }
                remove("deviceToken")
            }
            if (has("devicePin")) {
                data.apiPin = data.apiPin.toMutableMap().also {
                    it[data.symbol] = getString("devicePin")
                }
                remove("devicePin")
            }
        }
    }

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

                val privateKey = getPrivateKeyFromCert(
                        if (data.apiToken[data.symbol]?.get(0) == 'F') VULCAN_API_PASSWORD_FAKELOG else VULCAN_API_PASSWORD,
                        cert.getString("CertyfikatPfx") ?: ""
                )

                data.apiFingerprint = data.apiFingerprint.toMutableMap().also {
                    it[data.symbol] = cert.getString("CertyfikatKlucz")
                }
                data.apiToken = data.apiToken.toMutableMap().also {
                    it[data.symbol] = it[data.symbol]?.substring(0, 3)
                }
                data.apiPrivateKey = data.apiPrivateKey.toMutableMap().also {
                    it[data.symbol] = privateKey
                }
                data.loginStore.removeLoginData("certificatePfx")
                data.loginStore.removeLoginData("apiPin")
                onSuccess()
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        val deviceId = data.app.deviceId.padStart(16, '0')
        val loginStoreId = data.loginStore.id.toString(16).padStart(4, '0')
        val symbol = data.symbol?.crc16()?.toString(16)?.take(2) ?: "00"
        val uuid =
                deviceId.substring(0..7) +
                        "-" + deviceId.substring(8..11) +
                        "-" + deviceId.substring(12..15) +
                        "-" + loginStoreId +
                        "-" + symbol + "6f72616e7a"

        val deviceNameSuffix = " - nie usuwać"

        val szkolnyApi = SzkolnyApi(data.app)
        val firebaseToken = szkolnyApi.runCatching({
            getFirebaseToken("vulcan")
        }, onError = {
            // screw errors
        }) ?: data.app.config.sync.tokenVulcan

        Request.builder()
                .url("${data.apiUrl}$VULCAN_API_ENDPOINT_CERTIFICATE")
                .userAgent(VULCAN_API_USER_AGENT)
                .addHeader("RequestMobileType", "RegisterDevice")
                .addParameter("PIN", data.apiPin[data.symbol])
                .addParameter("TokenKey", data.apiToken[data.symbol])
                .addParameter("DeviceId", uuid)
                .addParameter("DeviceName", VULCAN_API_DEVICE_NAME.take(50 - deviceNameSuffix.length) + deviceNameSuffix)
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
                .addParameter("FirebaseTokenKey", firebaseToken ?: "")
                .postJson()
                .allowErrorCode(HTTP_BAD_REQUEST)
                .callback(callback)
                .build()
                .enqueue()
    }
}
