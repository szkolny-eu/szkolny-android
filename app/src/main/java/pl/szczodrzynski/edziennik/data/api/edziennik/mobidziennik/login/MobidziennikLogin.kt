/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
import pl.szczodrzynski.edziennik.utils.Utils

class MobidziennikLogin(val data: DataMobidziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikLogin"
    }

    private var cancelled = false

    init {
        nextLoginMethod(onSuccess)
    }

    private fun nextLoginMethod(onSuccess: () -> Unit) {
        if (data.targetLoginMethods.isEmpty()) {
            onSuccess()
            return
        }
        if (cancelled) {
            onSuccess()
            return
        }
        useLoginMethod(data.targetLoginMethods.removeAt(0)) { usedMethod ->
            data.progress(data.progressStep)
            if (usedMethod != null)
                data.loginMethods.add(usedMethod)
            nextLoginMethod(onSuccess)
        }
    }

    private fun useLoginMethod(loginMethod: LoginMethod, onSuccess: (usedMethod: LoginMethod?) -> Unit) {
        // this should never be true
        if (data.loginMethods.contains(loginMethod)) {
            onSuccess(null)
            return
        }
        Utils.d(TAG, "Using login method $loginMethod")
        when (loginMethod) {
            LoginMethod.MOBIDZIENNIK_WEB -> {
                data.startProgress(R.string.edziennik_progress_login_mobidziennik_web)
                MobidziennikLoginWeb(data) { onSuccess(loginMethod) }
            }
            LoginMethod.MOBIDZIENNIK_API2 -> {
                data.startProgress(R.string.edziennik_progress_login_mobidziennik_api2)
                MobidziennikLoginApi2(data) { onSuccess(loginMethod) }
            }
            else -> {}
        }
    }
}
