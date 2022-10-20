/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.db.enums.LoginMethod
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
        d(TAG, "Using login method $loginMethod")
        when (loginMethod) {
            LoginMethod.USOS_API -> {
                data.startProgress(R.string.edziennik_progress_login_usos_api)
                UsosLoginApi(data) { onSuccess(loginMethod) }
            }
            else -> {}
        }
    }
}
