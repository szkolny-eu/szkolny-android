/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-17
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_GDYNIA_WEB
import pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.DataGdynia
import pl.szczodrzynski.edziennik.utils.Utils

class GdyniaLogin(val data: DataGdynia, val onSuccess: () -> Unit) {
    companion object {
private const val TAG = "GdyniaLogin"
    }

    init {
        nextLoginMethod(onSuccess)
    }

    private var cancelled = false

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
            LOGIN_METHOD_GDYNIA_WEB -> {
                data.startProgress(R.string.edziennik_progress_login_gdynia_web)
                GdyniaLoginWeb(data) { onSuccess(loginMethodId) }
            }
        }
    }
}
