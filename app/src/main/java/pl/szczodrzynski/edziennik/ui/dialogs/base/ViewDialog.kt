/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-18.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.base

import android.view.View
import androidx.appcompat.app.AppCompatActivity

abstract class ViewDialog<V : View>(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null
) : BaseDialog(activity, onShowListener, onDismissListener) {

    protected lateinit var root: V
    protected abstract fun getRootView(): V

    final override fun getMessage() = null
    final override fun getMessageFormat() = null
    final override fun getView(): View {
        root = getRootView()
        return root
    }
}
