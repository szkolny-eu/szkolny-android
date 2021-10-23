/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-29.
 */

package pl.szczodrzynski.edziennik.ui.attendance

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.attendance.models.*
import pl.szczodrzynski.edziennik.ui.attendance.viewholder.*
import pl.szczodrzynski.edziennik.ui.grades.models.ExpandableItemModel
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder
import kotlin.coroutines.CoroutineContext

class AttendanceAdapter(
        val activity: AppCompatActivity,
        val type: Int,
        var onAttendanceClick: ((item: AttendanceFull) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "AttendanceAdapter"
        private const val ITEM_TYPE_ATTENDANCE = 0
        private const val ITEM_TYPE_DAY_RANGE = 1
        private const val ITEM_TYPE_MONTH = 2
        private const val ITEM_TYPE_SUBJECT = 3
        private const val ITEM_TYPE_TYPE = 4
        private const val ITEM_TYPE_EMPTY = 5
        const val STATE_CLOSED = 0
        const val STATE_OPENED = 1
    }

    private val app = activity.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = mutableListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_ATTENDANCE -> AttendanceViewHolder(inflater, parent)
            ITEM_TYPE_DAY_RANGE -> DayRangeViewHolder(inflater, parent)
            ITEM_TYPE_MONTH -> MonthViewHolder(inflater, parent)
            ITEM_TYPE_SUBJECT -> SubjectViewHolder(inflater, parent)
            ITEM_TYPE_TYPE -> TypeViewHolder(inflater, parent)
            ITEM_TYPE_EMPTY -> EmptyViewHolder(inflater, parent)
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AttendanceFull -> ITEM_TYPE_ATTENDANCE
            is AttendanceDayRange -> ITEM_TYPE_DAY_RANGE
            is AttendanceMonth -> ITEM_TYPE_MONTH
            is AttendanceSubject -> ITEM_TYPE_SUBJECT
            is AttendanceTypeGroup -> ITEM_TYPE_TYPE
            is AttendanceEmpty -> ITEM_TYPE_EMPTY
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    private val onClickListener = View.OnClickListener { view ->
        val model = view.getTag(R.string.tag_key_model)
        if (model is AttendanceFull) {
            onAttendanceClick?.invoke(model)
            return@OnClickListener
        }
        if (model !is ExpandableItemModel<*>)
            return@OnClickListener
        expandModel(model, view)
    }

    fun expandModel(model: ExpandableItemModel<*>?, view: View?, notifyAdapter: Boolean = true) {
        model ?: return
        val position = items.indexOf(model)
        if (position == -1)
            return

        view?.findViewById<View>(R.id.dropdownIcon)?.let { dropdownIcon ->
            ObjectAnimator.ofFloat(
                    dropdownIcon,
                    View.ROTATION,
                    if (model.state == STATE_CLOSED) 0f else 180f,
                    if (model.state == STATE_CLOSED) 180f else 0f
            ).setDuration(200).start();
        }

        if (model is AttendanceDayRange || model is AttendanceMonth || model is AttendanceTypeGroup) {
            // hide the preview, show summary
            val preview = view?.findViewById<View>(R.id.previewContainer)
            val summary = view?.findViewById<View>(R.id.summaryContainer)
            val percentage = view?.findViewById<View>(R.id.percentage)
            preview?.isInvisible = model.state == STATE_CLOSED
            summary?.isInvisible = model.state != STATE_CLOSED
            percentage?.isVisible = model.state != STATE_CLOSED
        }

        if (model.state == STATE_CLOSED) {

            val subItems = when {
                model.items.isEmpty() -> listOf(AttendanceEmpty())
                else -> model.items
            }

            model.state = STATE_OPENED
            items.addAll(position + 1, subItems.filterNotNull())
            if (notifyAdapter) notifyItemRangeInserted(position + 1, subItems.size)
        }
        else {
            val start = position + 1
            var end: Int = items.size
            for (i in start until items.size) {
                val model1 = items[i]
                val level = (model1 as? ExpandableItemModel<*>)?.level ?: 3
                if (level <= model.level) {
                    end = i
                    break
                } else {
                    if (model1 is ExpandableItemModel<*> && model1.state == STATE_OPENED) {
                        model1.state = STATE_CLOSED
                    }
                }
            }

            if (end != -1) {
                items.subList(start, end).clear()
                if (notifyAdapter) notifyItemRangeRemoved(start, end - start)
            }

            model.state = STATE_CLOSED
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder !is BindableViewHolder<*, *>)
            return

        val viewType = when (holder) {
            is AttendanceViewHolder -> ITEM_TYPE_ATTENDANCE
            is DayRangeViewHolder -> ITEM_TYPE_DAY_RANGE
            is MonthViewHolder -> ITEM_TYPE_MONTH
            is SubjectViewHolder -> ITEM_TYPE_SUBJECT
            is TypeViewHolder -> ITEM_TYPE_TYPE
            is EmptyViewHolder -> ITEM_TYPE_EMPTY
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
        holder.itemView.setTag(R.string.tag_key_view_type, viewType)
        holder.itemView.setTag(R.string.tag_key_position, position)
        holder.itemView.setTag(R.string.tag_key_model, item)

        when {
            holder is AttendanceViewHolder && item is AttendanceFull -> holder.onBind(activity, app, item, position, this)
            holder is DayRangeViewHolder && item is AttendanceDayRange -> holder.onBind(activity, app, item, position, this)
            holder is MonthViewHolder && item is AttendanceMonth -> holder.onBind(activity, app, item, position, this)
            holder is SubjectViewHolder && item is AttendanceSubject -> holder.onBind(activity, app, item, position, this)
            holder is TypeViewHolder && item is AttendanceTypeGroup -> holder.onBind(activity, app, item, position, this)
            holder is EmptyViewHolder && item is AttendanceEmpty -> holder.onBind(activity, app, item, position, this)
        }

        if (item !is AttendanceFull || onAttendanceClick != null)
            holder.itemView.setOnClickListener(onClickListener)
        else
            holder.itemView.setOnClickListener(null)
    }

    fun notifyItemChanged(model: Any) {
        startCoroutineTimer(1000L, 0L) {
            val index = items.indexOf(model)
            if (index != -1)
                notifyItemChanged(index)
        }
    }

    override fun getItemCount() = items.size
}
