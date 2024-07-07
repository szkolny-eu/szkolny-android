/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-19
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.firstlogin

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanWebMain
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe.VulcanHebeMain
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login.CufsCertificate
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login.VulcanLoginHebe
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login.VulcanLoginWebMain
import pl.szczodrzynski.edziennik.data.api.events.FirstLoginFinishedEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.ext.getJsonObject
import pl.szczodrzynski.edziennik.ext.getString

class VulcanFirstLogin(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        const val TAG = "VulcanFirstLogin"
    }

    private val web = VulcanWebMain(data, null)
    private val hebe = VulcanHebe(data, null)
    private val profileList = mutableListOf<Profile>()
    private val loginStoreId = data.loginStore.id
    private var firstProfileId = loginStoreId
    private val tryingSymbols = mutableListOf<String>()

    init {
        if (data.loginStore.mode == LoginMode.VULCAN_WEB) {
            VulcanLoginWebMain(data) {
                val xml = web.readCertificate() ?: run {
                    data.error(ApiError(TAG, ERROR_VULCAN_WEB_NO_CERTIFICATE))
                    return@VulcanLoginWebMain
                }
                val certificate = web.parseCertificate(xml)

                if (data.symbol != null && data.symbol != "default") {
                    tryingSymbols += data.symbol ?: "default"
                }
                else {

                    tryingSymbols += certificate.userInstances
                }

                checkSymbol(certificate)
            }
        }
        else {
            registerDeviceHebe {
                EventBus.getDefault().postSticky(FirstLoginFinishedEvent(profileList, data.loginStore))
                onSuccess()
            }
        }
    }

    private fun checkSymbol(certificate: CufsCertificate) {
        if (tryingSymbols.isEmpty()) {
            EventBus.getDefault().postSticky(FirstLoginFinishedEvent(profileList, data.loginStore))
            onSuccess()
            return
        }

        val result = web.postCertificate(certificate, tryingSymbols.removeAt(0)) { symbol, state ->
            when (state) {
                VulcanWebMain.STATE_NO_REGISTER -> {
                    checkSymbol(certificate)
                }
                VulcanWebMain.STATE_LOGGED_OUT -> data.error(ApiError(TAG, ERROR_VULCAN_WEB_LOGGED_OUT))
                VulcanWebMain.STATE_SUCCESS -> {
                    webRegisterDevice(symbol) {
                        checkSymbol(certificate)
                    }
                }
            }
        }

        // postCertificate returns false if the cert is not valid anymore
        if (!result) {
            data.error(ApiError(TAG, ERROR_VULCAN_WEB_CERTIFICATE_EXPIRED)
                    .withApiResponse(certificate.xml))
        }
    }

    private fun webRegisterDevice(symbol: String, onSuccess: () -> Unit) {
        web.getStartPage(symbol, postErrors = false) { _, schoolSymbols ->
            if (schoolSymbols.isEmpty()) {
                onSuccess()
                return@getStartPage
            }
            data.symbol = symbol
            val schoolSymbol = data.schoolSymbol ?: schoolSymbols.firstOrNull()
            web.webGetJson(TAG, VulcanWebMain.WEB_NEW, "$schoolSymbol/$VULCAN_WEB_ENDPOINT_REGISTER_DEVICE") { result, _ ->
                val json = result.getJsonObject("data")
                data.symbol = symbol
                data.apiToken = data.apiToken.toMutableMap().also {
                    it[symbol] = json.getString("TokenKey")
                }
                data.apiPin = data.apiPin.toMutableMap().also {
                    it[symbol] = json.getString("PIN")
                }
                registerDeviceHebe(onSuccess)
            }
        }
    }

    private fun registerDeviceHebe(onSuccess: () -> Unit) {
        VulcanLoginHebe(data) {
            VulcanHebeMain(data).getStudents(
                profile = null,
                profileList,
                loginStoreId,
                firstProfileId,
                onEmpty = {
                    EventBus.getDefault()
                        .postSticky(FirstLoginFinishedEvent(listOf(), data.loginStore))
                    onSuccess()
                },
                onSuccess = onSuccess
            )
        }
    }
}
