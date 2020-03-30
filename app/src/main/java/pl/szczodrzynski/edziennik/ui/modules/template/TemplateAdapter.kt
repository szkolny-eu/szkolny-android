/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.modules.template

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.TemplateListItemBinding
import pl.szczodrzynski.edziennik.onClick
import kotlin.coroutines.CoroutineContext

class TemplateAdapter(
        val activity: AppCompatActivity,
        val onItemClick: ((item: TemplateItem) -> Unit)? = null,
        val onItemButtonClick: ((item: TemplateItem) -> Unit)? = null
) : RecyclerView.Adapter<TemplateAdapter.ViewHolder>(), CoroutineScope {

    private val app = activity.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = listOf<TemplateItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = TemplateListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.b

        onItemClick?.let { listener ->
            b.root.onClick { listener(item) }
        }

        /*b.someButton.visibility = if (buttonVisible) View.VISIBLE else View.GONE
        onItemButtonClick?.let { listener ->
            b.someButton.onClick { listener(item) }
        }*/
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: TemplateListItemBinding) : RecyclerView.ViewHolder(b.root)

    data class TemplateItem(val text: String)
}
