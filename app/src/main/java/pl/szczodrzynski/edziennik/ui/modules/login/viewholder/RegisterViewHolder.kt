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
import pl.szczodrzynski.edziennik.databinding.LoginChooserItemBinding
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.modules.login.LoginChooserAdapter
import pl.szczodrzynski.edziennik.ui.modules.login.LoginInfo

class RegisterViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: LoginChooserItemBinding = LoginChooserItemBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<LoginInfo.Register, LoginChooserAdapter> {
    companion object {
        private const val TAG = "RegisterViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: LoginInfo.Register, position: Int, adapter: LoginChooserAdapter) {
        b.logo.setImageResource(item.registerLogo)
        b.name.setText(item.registerName)
        b.description.isVisible = false
    }
}
