/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.template

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.CODE_INTERNAL_LIBRUS_ACCOUNT_410
import pl.szczodrzynski.edziennik.data.api.edziennik.template.data.TemplateData
import pl.szczodrzynski.edziennik.data.api.edziennik.template.firstlogin.TemplateFirstLogin
import pl.szczodrzynski.edziennik.data.api.edziennik.template.login.TemplateLogin
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.prepare
import pl.szczodrzynski.edziennik.data.api.templateLoginMethods
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.utils.Utils.d

class Template(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Template"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataTemplate

    init {
        data = DataTemplate(app, profile, loginStore).apply {
            callback = wrapCallback(this@Template.callback)
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
    override fun sync(featureIds: List<Int>, viewId: Int?, onlyEndpoints: List<Int>?, arguments: JsonObject?) {
        data.arguments = arguments
        data.prepare(templateLoginMethods, TemplateFeatures, featureIds, viewId, onlyEndpoints)
        d(TAG, "LoginMethod IDs: ${data.targetLoginMethodIds}")
        d(TAG, "Endpoint IDs: ${data.targetEndpointIds}")
        TemplateLogin(data) {
            TemplateData(data) {
                completed()
            }
        }
    }

    override fun getMessage(message: MessageFull) {

    }

    override fun sendMessage(recipients: List<Teacher>, subject: String, text: String) {

    }

    override fun markAllAnnouncementsAsRead() {

    }

    override fun getAnnouncement(announcement: AnnouncementFull) {

    }

    override fun getAttachment(owner: Any, attachmentId: Long, attachmentName: String) {

    }

    override fun getRecipientList() {

    }

    override fun getEvent(eventFull: EventFull) {

    }

    override fun firstLogin() {
        TemplateFirstLogin(data) {
            completed()
        }
    }

    override fun cancel() {
        d(TAG, "Cancelled")
        data.cancel()
        callback.onCompleted()
    }

    private fun wrapCallback(callback: EdziennikCallback): EdziennikCallback {
        return object : EdziennikCallback {
            override fun onCompleted() {
                callback.onCompleted()
            }

            override fun onProgress(step: Float) {
                callback.onProgress(step)
            }

            override fun onStartProgress(stringRes: Int) {
                callback.onStartProgress(stringRes)
            }

            override fun onError(apiError: ApiError) {
                when (apiError.errorCode) {
                    in internalErrorList -> {
                        // finish immediately if the same error occurs twice during the same sync
                        callback.onError(apiError)
                    }
                    CODE_INTERNAL_LIBRUS_ACCOUNT_410 -> {
                        internalErrorList.add(apiError.errorCode)
                        loginStore.removeLoginData("refreshToken") // force a clean login
                        //loginLibrus()
                    }
                    else -> callback.onError(apiError)
                }
            }
        }
    }
}
