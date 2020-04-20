/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.template.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_TEMPLATE_API
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_TEMPLATE_WEB
import pl.szczodrzynski.edziennik.data.api.edziennik.template.DataTemplate
import pl.szczodrzynski.edziennik.utils.Utils

class TemplateLogin(val data: DataTemplate, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "TemplateLogin"
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
            LOGIN_METHOD_TEMPLATE_WEB -> {
                data.startProgress(R.string.edziennik_progress_login_template_web)
                TemplateLoginWeb(data) { onSuccess(loginMethodId) }
            }
            LOGIN_METHOD_TEMPLATE_API -> {
                data.startProgress(R.string.edziennik_progress_login_template_api)
                TemplateLoginApi(data) { onSuccess(loginMethodId) }
            }
        }
    }
}
