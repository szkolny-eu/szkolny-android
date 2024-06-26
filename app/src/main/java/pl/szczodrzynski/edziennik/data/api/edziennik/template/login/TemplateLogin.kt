/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.template.login

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.template.DataTemplate
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
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
            LoginMethod.TEMPLATE_WEB -> {
                data.startProgress(R.string.edziennik_progress_login_template_web)
                TemplateLoginWeb(data) { onSuccess(loginMethod) }
            }
            LoginMethod.TEMPLATE_API -> {
                data.startProgress(R.string.edziennik_progress_login_template_api)
                TemplateLoginApi(data) { onSuccess(loginMethod) }
            }
            else -> {}
        }
    }
}
