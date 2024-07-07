package pl.szczodrzynski.navlib.bottomsheet.items

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.navlib.R

data class BottomSheetSeparatorItem(override val isContextual: Boolean = true) : IBottomSheetItem<BottomSheetSeparatorItem.ViewHolder> {

    /*_                             _
     | |                           | |
     | |     __ _ _   _  ___  _   _| |_
     | |    / _` | | | |/ _ \| | | | __|
     | |___| (_| | |_| | (_) | |_| | |_
     |______\__,_|\__, |\___/ \__,_|\__|
                   __/ |
                  |__*/
    override var id: Int = -1
    override val viewType: Int
        get() = 2
    override val layoutId: Int
        get() = R.layout.nav_bs_item_separator

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun bindViewHolder(viewHolder: ViewHolder) {

    }
}