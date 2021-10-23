/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-10.
 */

package pl.szczodrzynski.edziennik.ui.login.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.LoginPlatformItemBinding
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.login.LoginInfo
import pl.szczodrzynski.edziennik.ui.login.LoginPlatformAdapter

class PlatformViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: LoginPlatformItemBinding = LoginPlatformItemBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<LoginInfo.Platform, LoginPlatformAdapter> {
    companion object {
        private const val TAG = "PlatformViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: LoginInfo.Platform, position: Int, adapter: LoginPlatformAdapter) {
        b.logo.load(item.icon)
        b.name.text = item.name
        b.description.text = item.description
        b.description.isVisible = item.description != null
        b.screenshotButton.isVisible = false
    }
}
