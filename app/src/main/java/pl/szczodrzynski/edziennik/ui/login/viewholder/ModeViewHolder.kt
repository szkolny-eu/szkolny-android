/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-10.
 */

package pl.szczodrzynski.edziennik.ui.login.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.LoginChooserModeItemBinding
import pl.szczodrzynski.edziennik.ext.resolveColor
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.login.LoginChooserAdapter
import pl.szczodrzynski.edziennik.ui.login.LoginInfo

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

        b.badge.isVisible = item.isRecommended || item.isDevOnly || item.isTesting
        if (item.isRecommended) {
            b.badge.setText(R.string.login_chooser_mode_recommended)
            b.badge.background.setTintColor(R.color.md_blue_300.resolveColor(app))
        }
        if (item.isTesting) {
            b.badge.setText(R.string.login_chooser_mode_testing)
            b.badge.background.setTintColor(R.color.md_yellow_300.resolveColor(app))
        }
        if (item.isDevOnly) {
            b.badge.setText(R.string.login_chooser_mode_dev_only)
            b.badge.background.setTintColor(R.color.md_red_300.resolveColor(app))
        }
    }
}
