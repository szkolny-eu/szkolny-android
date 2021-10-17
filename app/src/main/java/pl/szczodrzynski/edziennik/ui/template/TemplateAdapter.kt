/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.template

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.databinding.TemplateListItemBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.coroutines.CoroutineContext

class TemplateAdapter(
        val activity: AppCompatActivity,
        val onItemClick: ((item: Notification) -> Unit)? = null
) : RecyclerView.Adapter<TemplateAdapter.ViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "TemplateAdapter"
    }

    private val app = activity.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = listOf<Notification>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = TemplateListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.b

        val date = Date.fromMillis(item.addedDate).formattedString
        val colorSecondary = android.R.attr.textColorSecondary.resolveAttr(activity)

        b.title.text = item.text
        b.profileDate.text = listOf(
                item.profileName ?: "",
                " • ",
                date
        ).concat().asColoredSpannable(colorSecondary)
        b.type.text = activity.getNotificationTitle(item.type)

        onItemClick?.let { listener ->
            b.root.onClick { listener(item) }
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: TemplateListItemBinding) : RecyclerView.ViewHolder(b.root)
}
