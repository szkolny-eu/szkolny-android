/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
import pl.szczodrzynski.edziennik.utils.Utils

class LibrusLogin(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LibrusLogin"
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
            LoginMethod.LIBRUS_PORTAL -> {
                data.startProgress(R.string.edziennik_progress_login_librus_portal)
                LibrusLoginPortal(data) { onSuccess(loginMethod) }
            }
            LoginMethod.LIBRUS_API -> {
                data.startProgress(R.string.edziennik_progress_login_librus_api)
                LibrusLoginApi(data) { onSuccess(loginMethod) }
            }
            LoginMethod.LIBRUS_SYNERGIA -> {
                data.startProgress(R.string.edziennik_progress_login_librus_synergia)
                LibrusLoginSynergia(data) { onSuccess(loginMethod) }
            }
            LoginMethod.LIBRUS_MESSAGES -> {
                data.startProgress(R.string.edziennik_progress_login_librus_messages)
                LibrusLoginMessages(data) { onSuccess(loginMethod) }
            }
            else -> {}
        }
    }
}
