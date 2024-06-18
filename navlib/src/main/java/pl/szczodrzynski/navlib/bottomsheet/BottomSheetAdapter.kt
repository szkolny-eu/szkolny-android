package pl.szczodrzynski.navlib.bottomsheet

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.navlib.R
import pl.szczodrzynski.navlib.bottomsheet.items.IBottomSheetItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem

class BottomSheetAdapter(val items: List<IBottomSheetItem<*>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewHolderProvider = ViewHolderProvider()

    init {
        viewHolderProvider.registerViewHolderFactory(1, R.layout.nav_bs_item_primary) { itemView ->
            BottomSheetPrimaryItem.ViewHolder(itemView)
        }
        viewHolderProvider.registerViewHolderFactory(2, R.layout.nav_bs_item_separator) { itemView ->
            BottomSheetSeparatorItem.ViewHolder(itemView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return viewHolderProvider.provideViewHolder(viewGroup = parent, viewType = viewType)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        items[position].bindViewHolder(viewHolder = holder)
    }
}