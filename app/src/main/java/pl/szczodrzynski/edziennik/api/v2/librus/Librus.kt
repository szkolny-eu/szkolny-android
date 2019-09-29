/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus

import android.content.Context
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.AppError
import pl.szczodrzynski.edziennik.api.interfaces.SyncCallback
import pl.szczodrzynski.edziennik.api.v2.CODE_INTERNAL_LIBRUS_ACCOUNT_410
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikInterface
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile
import pl.szczodrzynski.edziennik.datamodels.ProfileFull

class Librus(val app: App, val profile: Profile?, val loginStore: LoginStore, val callback: EdziennikCallback) : EdziennikInterface {

    val internalErrorList = mutableListOf<Int>()
    val data: DataLibrus

    init {
        data = DataLibrus(app, profile, loginStore).apply {
            callback = wrapCallback(this@Librus.callback)
        }
        data.satisfyLoginMethods()
    }

    override fun sync(featureIds: List<Int>) {

    }

    override fun getMessage(messageId: Int) {

    }

    private fun wrapCallback(callback: SyncCallback): SyncCallback {
        return object : SyncCallback {
            override fun onSuccess(activityContext: Context?, profileFull: ProfileFull?) {
                callback.onSuccess(activityContext, profileFull)
            }

            override fun onProgress(progressStep: Int) {
                callback.onProgress(progressStep)
            }

            override fun onActionStarted(stringResId: Int) {
                callback.onActionStarted(stringResId)
            }

            override fun onLoginFirst(profileList: MutableList<Profile>?, loginStore: LoginStore?) {
                callback.onLoginFirst(profileList, loginStore)
            }

            override fun onError(activityContext: Context?, error: AppError) {
                when (error.errorCode) {
                    in internalErrorList -> {
                        // finish immediately if the same error occurs twice during the same sync
                        callback.onError(activityContext, error)
                    }
                    CODE_INTERNAL_LIBRUS_ACCOUNT_410 -> {
                        internalErrorList.add(error.errorCode)
                        loginStore.removeLoginData("refreshToken") // force a clean login
                        //loginLibrus()
                    }
                    else -> callback.onError(activityContext, error)
                }
            }
        }
    }
}