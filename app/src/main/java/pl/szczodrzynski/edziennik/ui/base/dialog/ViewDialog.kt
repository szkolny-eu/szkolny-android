/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-18.
 */

package pl.szczodrzynski.edziennik.ui.base.dialog

import android.view.View
import androidx.appcompat.app.AppCompatActivity

abstract class ViewDialog<V : View>(
    activity: AppCompatActivity,
) : BaseDialog<Any>(activity) {

    protected lateinit var root: V
    protected abstract fun getRootView(): V

    final override fun getMessage() = null
    final override fun getMessageFormat() = null
    final override fun getView(): View {
        root = getRootView()
        return root
    }
}
