/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6. 
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_VULCAN_API
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanData
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api.VulcanApiMessagesChangeStatus
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api.VulcanApiSendMessage
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.firstlogin.VulcanFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login.VulcanLogin
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.prepare
import pl.szczodrzynski.edziennik.data.api.prepareFor
import pl.szczodrzynski.edziennik.data.api.vulcanLoginMethods
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.Utils.d

class Vulcan(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Vulcan"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataVulcan
    private var afterLogin: (() -> Unit)? = null

    init {
        data = DataVulcan(app, profile, loginStore).apply {
            callback = wrapCallback(this@Vulcan.callback)
            satisfyLoginMethods()
        }
    }

    private fun completed() {
        data.saveData()
        callback.onCompleted()
    }

    /*    _______ _                     _                  _ _   _
         |__   __| |              /\   | |                (_) | | |
            | |  | |__   ___     /  \  | | __ _  ___  _ __ _| |_| |__  _ __ ___
            | |  | '_ \ / _ \   / /\ \ | |/ _` |/ _ \| '__| | __| '_ \| '_ ` _ \
            | |  | | | |  __/  / ____ \| | (_| | (_) | |  | | |_| | | | | | | | |
            |_|  |_| |_|\___| /_/    \_\_|\__, |\___/|_|  |_|\__|_| |_|_| |_| |_|
                                           __/ |
                                          |__*/
    override fun sync(featureIds: List<Int>, viewId: Int?, arguments: JsonObject?) {
        data.arguments = arguments
        data.prepare(vulcanLoginMethods, VulcanFeatures, featureIds, viewId)
        login()
    }

    private fun login(loginMethodId: Int? = null, afterLogin: (() -> Unit)? = null) {
        d(TAG, "Trying to login with ${data.targetLoginMethodIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        loginMethodId?.let { data.prepareFor(vulcanLoginMethods, it) }
        afterLogin?.let { this.afterLogin = it }
        VulcanLogin(data) {
            data()
        }
    }

    private fun data() {
        d(TAG, "Endpoint IDs: ${data.targetEndpointIds}")
        if (internalErrorList.isNotEmpty()) {
            d(TAG, "  - Internal errors:")
            internalErrorList.forEach { d(TAG, "      - code $it") }
        }
        afterLogin?.invoke() ?: VulcanData(data) {
            completed()
        }
    }

    override fun getMessage(message: MessageFull) {
        login(LOGIN_METHOD_VULCAN_API) {
            VulcanApiMessagesChangeStatus(data, message) {
                completed()
            }
        }
    }

    override fun sendMessage(recipients: List<Teacher>, subject: String, text: String) {
        login(LOGIN_METHOD_VULCAN_API) {
            VulcanApiSendMessage(data, recipients, subject, text) {
                completed()
            }
        }
    }

    override fun markAllAnnouncementsAsRead() {

    }

    override fun getAnnouncement(announcement: AnnouncementFull) {

    }

    override fun getAttachment(message: Message, attachmentId: Long, attachmentName: String) {
        
    }

    override fun getRecipientList() {

    }

    override fun firstLogin() { VulcanFirstLogin(data) { completed() } }
    override fun cancel() {
        d(TAG, "Cancelled")
        data.cancel()
    }

    private fun wrapCallback(callback: EdziennikCallback): EdziennikCallback {
        return object : EdziennikCallback {
            override fun onCompleted() { callback.onCompleted() }
            override fun onProgress(step: Float) { callback.onProgress(step) }
            override fun onStartProgress(stringRes: Int) { callback.onStartProgress(stringRes) }
            override fun onError(apiError: ApiError) {
                if (apiError.errorCode in internalErrorList) {
                    // finish immediately if the same error occurs twice during the same sync
                    callback.onError(apiError)
                    return
                }
                internalErrorList.add(apiError.errorCode)
                when (apiError.errorCode) {
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
