/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-16.
 */

package pl.szczodrzynski.edziennik.ui.modules.login

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.LoginSummaryItemBinding
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.trigger
import kotlin.coroutines.CoroutineContext

class LoginSummaryAdapter(
        val activity: LoginActivity,
        val onSelectionChanged: ((item: Item) -> Unit)? = null
) : RecyclerView.Adapter<LoginSummaryAdapter.ViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "LoginSummaryAdapter"
    }

    private val app = activity.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = listOf<Item>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(LoginSummaryItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.b
        val profile = item.profile
        val loginStore = activity.loginStores.firstOrNull { it.id == profile.loginStoreId }
                ?: return

        val loginType = loginStore.type
        val register = LoginInfo.list.firstOrNull { it.loginType == loginType } ?: return
        val loginMode = loginStore.mode
        val mode = register.loginModes.firstOrNull { it.loginMode == loginMode } ?: return

        b.profileName.text = profile.name
        b.profileDetails.text = profile.subname
        b.checkBox.isChecked = item.isSelected
        b.modeIcon.setImageResource(mode.icon)

        if (profile.isParent) {
            b.accountType.setText(R.string.account_type_parent)
        } else {
            b.accountType.setText(R.string.account_type_child)
        }

        b.root.onClick {
            b.checkBox.trigger()
        }
        b.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            onSelectionChanged?.invoke(item)
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: LoginSummaryItemBinding) : RecyclerView.ViewHolder(b.root)

    class Item(val profile: Profile, var isSelected: Boolean = true)
}
