/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment.Companion.swapCards
import pl.szczodrzynski.edziennik.utils.SwipeRefreshLayoutNoIndicator

class CardItemTouchHelperCallback(private val cardAdapter: HomeCardAdapter, private val refreshLayout: SwipeRefreshLayoutNoIndicator?) : ItemTouchHelper.Callback() {
    companion object {
        private const val TAG = "CardItemTouchHelperCallback"
        private const val DRAG_FLAGS = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        private const val SWIPE_FLAGS = 0
    }

    private var dragCardView: MaterialCardView? = null

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return makeMovementFlags(DRAG_FLAGS, SWIPE_FLAGS)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition

        swapCards(fromPosition, toPosition, cardAdapter)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            dragCardView = viewHolder.itemView as MaterialCardView
            dragCardView?.isDragged = true
            refreshLayout?.isEnabled = false
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && dragCardView != null) {
            refreshLayout?.isEnabled = true
            dragCardView?.isDragged = false
            dragCardView = null
        }
    }
}