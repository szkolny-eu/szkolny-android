package pl.szczodrzynski.navlib.bottomsheet.items

import android.view.View
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlin.reflect.KClass

interface IBottomSheetItem<T> {

    val isContextual: Boolean
    var id: Int
    val viewType: Int
    val layoutId: Int

    fun bindViewHolder(viewHolder: T)
    fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        bindViewHolder(viewHolder as T)
    }
}