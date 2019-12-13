/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25. 
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_IDZIENNIK_API
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_IDZIENNIK_WEB
import pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.utils.Utils

class IdziennikLogin(val data: DataIdziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "IdziennikLogin"
    }

    private var cancelled = false

    init {
        nextLoginMethod(onSuccess)
    }

    private fun nextLoginMethod(onSuccess: () -> Unit) {
        if (data.targetLoginMethodIds.isEmpty()) {
            onSuccess()
            return
        }
        if (cancelled) {
            onSuccess()
            return
        }
        useLoginMethod(data.targetLoginMethodIds.removeAt(0)) { usedMethodId ->
            data.progress(data.progressStep)
            if (usedMethodId != -1)
                data.loginMethods.add(usedMethodId)
            nextLoginMethod(onSuccess)
        }
    }

    private fun useLoginMethod(loginMethodId: Int, onSuccess: (usedMethodId: Int) -> Unit) {
        // this should never be true
        if (data.loginMethods.contains(loginMethodId)) {
            onSuccess(-1)
            return
        }
        Utils.d(TAG, "Using login method $loginMethodId")
        when (loginMethodId) {
            LOGIN_METHOD_IDZIENNIK_WEB -> {
                data.startProgress(R.string.edziennik_progress_login_idziennik_web)
                IdziennikLoginWeb(data) { onSuccess(loginMethodId) }
            }
            LOGIN_METHOD_IDZIENNIK_API -> {
                data.startProgress(R.string.edziennik_progress_login_idziennik_api)
                IdziennikLoginApi(data) { onSuccess(loginMethodId) }
            }
        }
    }
}
