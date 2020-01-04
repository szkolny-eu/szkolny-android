/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-3.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.databinding.RowLoginProfileListItemBinding
import pl.szczodrzynski.edziennik.joinNotNullStrings
import pl.szczodrzynski.edziennik.onClick

class LoginSummaryProfileAdapter(
        val context: Context,
        val items: List<Item>,
        val onSelectionChanged: ((item: Item) -> Unit)? = null
) : RecyclerView.Adapter<LoginSummaryProfileAdapter.ViewHolder>() {

    private val app by lazy { context.applicationContext as App }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = RowLoginProfileListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val profile = item.profile
        val b = holder.b

        b.textView.text = profile.name
        b.checkBox.isChecked = item.isSelected

        val registerIcon = when (profile.loginStoreType) {
            LOGIN_TYPE_MOBIDZIENNIK -> R.drawable.logo_mobidziennik
            LOGIN_TYPE_LIBRUS -> R.drawable.logo_librus
            LOGIN_TYPE_IDZIENNIK -> R.drawable.logo_idziennik
            LOGIN_TYPE_VULCAN -> R.drawable.logo_vulcan
            LOGIN_TYPE_EDUDZIENNIK -> R.drawable.logo_edudziennik
            else -> null
        }
        if (registerIcon == null)
            b.registerIcon.visibility = View.GONE
        else {
            b.registerIcon.visibility = View.VISIBLE
            b.registerIcon.setImageResource(registerIcon)
        }

        if (profile.isParent) {
            b.accountType.setText(R.string.login_summary_account_parent)
        } else {
            b.accountType.setText(R.string.login_summary_account_child)
        }

        val schoolYearName = "${profile.studentSchoolYearStart}/${profile.studentSchoolYearStart+1}"
        b.textDetails.text = joinNotNullStrings(
                " - ",
                profile.studentClassName,
                schoolYearName
        )

        b.root.onClick {
            b.checkBox.performClick()
        }
        b.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            onSelectionChanged?.invoke(item)
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: RowLoginProfileListItemBinding) : RecyclerView.ViewHolder(b.root)

    class Item(val profile: Profile, var isSelected: Boolean = true)
}
