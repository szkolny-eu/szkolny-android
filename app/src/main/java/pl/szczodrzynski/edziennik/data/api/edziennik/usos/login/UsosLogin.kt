/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_USOS_API
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.utils.Utils.d

class UsosLogin(val data: DataUsos, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "UsosLogin"
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
        d(TAG, "Using login method $loginMethodId")
        when (loginMethodId) {
            LOGIN_METHOD_USOS_API -> {
                data.startProgress(R.string.edziennik_progress_login_usos_api)
                UsosLoginApi(data) { onSuccess(loginMethodId) }
            }
        }
    }
}
