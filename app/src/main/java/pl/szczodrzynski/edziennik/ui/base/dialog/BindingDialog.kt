/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-18.
 */

package pl.szczodrzynski.edziennik.ui.base.dialog

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BindingDialog<B : ViewBinding>(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : ViewDialog<View>(activity, onShowListener, onDismissListener) {

    protected lateinit var b: B
    protected abstract fun inflate(layoutInflater: LayoutInflater): B

    final override fun getRootView(): View {
        b = inflate(activity.layoutInflater)
        return b.root
    }
}
