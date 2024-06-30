/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6. 
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
import pl.szczodrzynski.edziennik.utils.Utils

class VulcanLogin(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "VulcanLogin"
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
            LoginMethod.VULCAN_WEB_MAIN -> {
                data.startProgress(R.string.edziennik_progress_login_vulcan_web_main)
                VulcanLoginWebMain(data) { onSuccess(loginMethod) }
            }
            LoginMethod.VULCAN_HEBE -> {
                data.startProgress(R.string.edziennik_progress_login_vulcan_api)
                VulcanLoginHebe(data) { onSuccess(loginMethod) }
            }
            else -> {}
        }
    }
}
