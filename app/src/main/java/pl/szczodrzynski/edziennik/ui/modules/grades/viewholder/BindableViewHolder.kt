/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.modules.grades.viewholder

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.App

interface BindableViewHolder<T, A> {
    fun onBind(activity: AppCompatActivity, app: App, item: T, position: Int, adapter: A)
}
