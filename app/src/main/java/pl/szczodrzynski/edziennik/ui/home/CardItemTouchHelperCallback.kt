/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.home

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import pl.szczodrzynski.edziennik.ui.home.HomeFragment.Companion.removeCard
import pl.szczodrzynski.edziennik.ui.home.HomeFragment.Companion.swapCards

class CardItemTouchHelperCallback(
    private val cardAdapter: HomeCardAdapter,
    private val onCanRefresh: ((canRefresh: Boolean) -> Unit)?,
) : ItemTouchHelper.Callback() {
    companion object {
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
        val position = viewHolder.adapterPosition
        removeCard(position, cardAdapter)
        cardAdapter.items.removeAt(position)
        cardAdapter.notifyItemRemoved(position)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (viewHolder != null && (actionState == ACTION_STATE_DRAG || actionState == ACTION_STATE_SWIPE)) {
            dragCardView = viewHolder.itemView as MaterialCardView
            dragCardView?.isDragged = true
            onCanRefresh?.invoke(false)
        }
        else if (actionState == ACTION_STATE_IDLE && dragCardView != null) {
            onCanRefresh?.invoke(true)
            dragCardView?.isDragged = false
            dragCardView = null
        }
    }
}
