/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import pl.szczodrzynski.edziennik.R

class HomeCardAdapter(var items: MutableList<HomeCard>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TAG = "HomeCardAdapter"
    }

    var itemTouchHelper: ItemTouchHelper? = null

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(itemTouchHelper)
        items[position].bind(position, holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        items.getOrNull(holder.adapterPosition)?.unbind(holder.adapterPosition, holder as ViewHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.card_home, parent, false) as MaterialCardView
        )
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(val root: MaterialCardView) : RecyclerView.ViewHolder(root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(itemTouchHelper: ItemTouchHelper?) {
            /*root.setOnTouchListener { _: View?, event: MotionEvent ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper?.startDrag(this)
                    return@setOnTouchListener true
                }
                false
            }*/
        }
    }
}