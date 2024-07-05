/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-18.
 */

package pl.szczodrzynski.edziennik.ui.base.dialog

import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R

abstract class ConfigDialog<B : ViewBinding>(
    activity: AppCompatActivity,
    private val reloadOnDismiss: Boolean = true,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<B>(activity, onShowListener, onDismissListener) {

    final override fun getPositiveButtonText() = R.string.ok
    final override suspend fun onShow() = Unit

    protected val config by lazy { app.config.grades }

    protected open suspend fun loadConfig() = Unit
    protected open suspend fun saveConfig() = Unit
    protected open fun initView() = Unit

    final override suspend fun onBeforeShow(): Boolean {
        initView()
        loadConfig()
        return true
    }

    final override suspend fun onDismiss() {
        saveConfig()
        if (reloadOnDismiss && activity is MainActivity)
            activity.reloadTarget()
    }
}
