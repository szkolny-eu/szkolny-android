/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-10.
 */

package pl.szczodrzynski.edziennik.ui.modules.login.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.LoginChooserModeItemBinding
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.modules.login.LoginChooserAdapter
import pl.szczodrzynski.edziennik.ui.modules.login.LoginInfo

class ModeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: LoginChooserModeItemBinding = LoginChooserModeItemBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<LoginInfo.Mode, LoginChooserAdapter> {
    companion object {
        private const val TAG = "ModeViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: LoginInfo.Mode, position: Int, adapter: LoginChooserAdapter) {
        b.logo.setImageResource(item.icon)
        b.name.setText(item.name)
        if (item.hintText == null) {
            b.description.isVisible = false
        }
        else {
            b.description.isVisible = true
            b.description.setText(item.hintText)
        }
        b.hint.isVisible = false
    }
}
