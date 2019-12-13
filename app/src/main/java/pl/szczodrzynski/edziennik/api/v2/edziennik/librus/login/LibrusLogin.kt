/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.librus.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_LIBRUS_API
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_LIBRUS_MESSAGES
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_LIBRUS_PORTAL
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_LIBRUS_SYNERGIA
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.DataLibrus
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
            LOGIN_METHOD_LIBRUS_PORTAL -> {
                data.startProgress(R.string.edziennik_progress_login_librus_portal)
                LibrusLoginPortal(data) { onSuccess(loginMethodId) }
            }
            LOGIN_METHOD_LIBRUS_API -> {
                data.startProgress(R.string.edziennik_progress_login_librus_api)
                LibrusLoginApi(data) { onSuccess(loginMethodId) }
            }
            LOGIN_METHOD_LIBRUS_SYNERGIA -> {
                data.startProgress(R.string.edziennik_progress_login_librus_synergia)
                LibrusLoginSynergia(data) { onSuccess(loginMethodId) }
            }
            LOGIN_METHOD_LIBRUS_MESSAGES -> {
                data.startProgress(R.string.edziennik_progress_login_librus_messages)
                LibrusLoginMessages(data) { onSuccess(loginMethodId) }
            }
        }
    }
}
