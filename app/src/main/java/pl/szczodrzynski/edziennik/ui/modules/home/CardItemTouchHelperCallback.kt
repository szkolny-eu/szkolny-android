/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment.Companion.removeCard
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment.Companion.swapCards
import pl.szczodrzynski.edziennik.utils.SwipeRefreshLayoutNoIndicator

class CardItemTouchHelperCallback(private val cardAdapter: HomeCardAdapter, private val refreshLayout: SwipeRefreshLayoutNoIndicator?) : ItemTouchHelper.Callback() {
    companion object {
        private const val TAG = "CardItemTouchHelperCallback"
        private const val DRAG_FLAGS = UP or DOWN
        private const val SWIPE_FLAGS = LEFT
    }

    private var dragCardView: MaterialCardView? = null

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return makeMovementFlags(DRAG_FLAGS, SWIPE_FLAGS)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition

        return swapCards(fromPosition, toPosition, cardAdapter)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        removeCard(viewHolder.adapterPosition)
        cardAdapter.items.removeAt(viewHolder.adapterPosition)
        cardAdapter.notifyItemRemoved(viewHolder.adapterPosition)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (viewHolder != null && (actionState == ACTION_STATE_DRAG || actionState == ACTION_STATE_SWIPE)) {
            dragCardView = viewHolder.itemView as MaterialCardView
            dragCardView?.isDragged = true
            refreshLayout?.isEnabled = false
        }
        else if (actionState == ACTION_STATE_IDLE && dragCardView != null) {
            refreshLayout?.isEnabled = true
            dragCardView?.isDragged = false
            dragCardView = null
        }
    }
}
