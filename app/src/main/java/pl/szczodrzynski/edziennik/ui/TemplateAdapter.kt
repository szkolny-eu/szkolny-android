/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-19.
 */

package pl.szczodrzynski.edziennik.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.TemplateListItemBinding
import pl.szczodrzynski.edziennik.onClick

class TemplateAdapter(
        val context: Context,
        val onItemClick: ((item: TemplateItem) -> Unit)? = null,
        val onItemButtonClick: ((item: TemplateItem) -> Unit)? = null
) : RecyclerView.Adapter<TemplateAdapter.ViewHolder>() {

    private val app by lazy { context.applicationContext as App }

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
