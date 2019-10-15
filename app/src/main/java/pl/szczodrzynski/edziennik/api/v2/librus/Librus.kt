/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus

import android.util.Log
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.CODE_INTERNAL_LIBRUS_ACCOUNT_410
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusData
import pl.szczodrzynski.edziennik.api.v2.librus.firstlogin.LibrusFirstLogin
import pl.szczodrzynski.edziennik.api.v2.librus.login.LibrusLogin
import pl.szczodrzynski.edziennik.api.v2.librusLoginMethods
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.api.v2.prepare
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.utils.Utils

class Librus(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {
    companion object {
        private const val TAG = "Librus"
    }

    val internalErrorList = mutableListOf<Int>()
    val data: DataLibrus

    init {
        data = DataLibrus(app, profile, loginStore).apply {
            callback = wrapCallback(this@Librus.callback)
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
    override fun sync(featureIds: List<Int>, viewId: Int?) {
        data.prepare(librusLoginMethods, LibrusFeatures, featureIds, viewId)
        Log.d(TAG, "LoginMethod IDs: ${data.targetLoginMethodIds}")
        Log.d(TAG, "Endpoint IDs: ${data.targetEndpointIds}")
        LibrusLogin(data) {
            LibrusData(data) {
                completed()
            }
        }
    }

    override fun getMessage(messageId: Int) {

    }

    override fun firstLogin() {
        LibrusFirstLogin(data) {
            completed()
        }
    }

    override fun cancel() {
        Utils.d(TAG, "Cancelled")
        data.cancel()
    }

    private fun wrapCallback(callback: EdziennikCallback): EdziennikCallback {
        return object : EdziennikCallback {
            override fun onCompleted() {
                callback.onCompleted()
            }

            override fun onProgress(step: Int) {
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