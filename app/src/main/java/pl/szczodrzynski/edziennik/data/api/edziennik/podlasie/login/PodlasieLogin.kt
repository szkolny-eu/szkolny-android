/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_PODLASIE_API
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.utils.Utils

class PodlasieLogin(val data: DataPodlasie, val onSuccess: () -> Unit) {
    companion object {
        const val TAG = "PodlasieLogin"
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
            LOGIN_METHOD_PODLASIE_API -> {
                data.startProgress(R.string.edziennik_progress_login_podlasie_api)
                PodlasieLoginApi(data) { onSuccess(loginMethodId) }
            }
        }
    }
}
