/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.LOGIN_METHOD_MOBIDZIENNIK_WEB
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.DataMobidziennik
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
        if (data.targetLoginMethodIds.isEmpty()) {
            onSuccess()
            return
        }
        useLoginMethod(data.targetLoginMethodIds.removeAt(0)) { usedMethodId ->
            if (usedMethodId != -1)
                data.loginMethods.add(usedMethodId)
            if (cancelled) {
                onSuccess()
                return@useLoginMethod
            }
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
            LOGIN_METHOD_MOBIDZIENNIK_WEB -> {
                data.startProgress(R.string.edziennik_progress_login_mobidziennik_web)
                MobidziennikLoginWeb(data) { onSuccess(loginMethodId) }
            }
        }
    }
}