package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login

import com.google.gson.JsonObject
import io.github.wulkanowy.signer.hebe.generateKeyPair
import pl.szczodrzynski.edziennik.JsonObject
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_DATA_MISSING
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_DEVICE_NAME
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_REGISTER_NEW
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

class VulcanLoginHebe(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "VulcanLoginHebe"
    }

    init { run {
        // i'm sure this does something useful
        // not quite sure what, though
        if (data.studentSemesterNumber == 1 && data.semester1Id == 0)
            data.semester1Id = data.studentSemesterNumber
        if (data.studentSemesterNumber == 2 && data.semester2Id == 0)
            data.semester2Id = data.studentSemesterNumber

        copyFromLoginStore()

        if (data.profile != null && data.isApiLoginValid()) {
            onSuccess()
        }
        else {
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
            // map form inputs to the symbol
            if (has("symbol")) {
                data.symbol = getString("symbol")
                remove("symbol")
            }
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
        val szkolnyApi = SzkolnyApi(data.app)
        val hebe = VulcanHebe(data, null)

        if (data.hebePublicKey == null || data.hebePrivateKey == null || data.hebePublicHash == null) {
            val (publicPem, privatePem, publicHash) = generateKeyPair()
            data.hebePublicKey = publicPem
            data.hebePrivateKey = privatePem
            data.hebePublicHash = publicHash
        }

        val firebaseToken = szkolnyApi.runCatching({
            getFirebaseToken("vulcan")
        }, onError = {
            // screw errors
        }) ?: data.app.config.sync.tokenVulcan

        hebe.apiPost(
            TAG,
            VULCAN_HEBE_ENDPOINT_REGISTER_NEW,
            payload = JsonObject(
                "OS" to "Android",
                "PIN" to data.apiPin[data.symbol],
                "Certificate" to data.hebePublicKey,
                "CertificateType" to "RSA_PEM",
                "DeviceModel" to VULCAN_API_DEVICE_NAME,
                "SecurityToken" to data.apiToken[data.symbol],
                "SelfIdentifier" to data.buildDeviceId(),
                "CertificateThumbprint" to data.hebePublicHash
            ),
            baseUrl = true,
            firebaseToken = firebaseToken
        ) { _: JsonObject, _ ->
            data.apiToken = data.apiToken.toMutableMap().also {
                it[data.symbol] = it[data.symbol]?.substring(0, 3)
            }
            data.loginStore.removeLoginData("apiPin")
            onSuccess()
        }
    }
}
