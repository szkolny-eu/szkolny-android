/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.modules.grades

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.ui.modules.grades.models.*
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.*

class GradesAdapter(
        val activity: AppCompatActivity,
        var onGradeClick: ((item: GradeFull) -> Unit)? = null,
        var onGradesEditorClick: ((subject: GradesSubject, semester: GradesSemester) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TAG = "GradesAdapter"
        private const val ITEM_TYPE_SUBJECT = 0
        private const val ITEM_TYPE_SEMESTER = 1
        private const val ITEM_TYPE_EMPTY = 2
        private const val ITEM_TYPE_GRADE = 3
        private const val ITEM_TYPE_STATS = 4
        const val STATE_CLOSED = 0
        const val STATE_OPENED = 1
    }

    var items = mutableListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_SUBJECT -> SubjectViewHolder(inflater, parent)
            ITEM_TYPE_SEMESTER -> SemesterViewHolder(inflater, parent)
            ITEM_TYPE_EMPTY -> EmptyViewHolder(inflater, parent)
            ITEM_TYPE_GRADE -> GradeViewHolder(inflater, parent)
            ITEM_TYPE_STATS -> StatsViewHolder(inflater, parent)
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is GradesSubject -> ITEM_TYPE_SUBJECT
            is GradesSemester -> ITEM_TYPE_SEMESTER
            is GradesEmpty -> ITEM_TYPE_EMPTY
            is Grade -> ITEM_TYPE_GRADE
            is GradesStats -> ITEM_TYPE_STATS
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    private val onClickListener = View.OnClickListener { view ->
        val model = view.getTag(R.string.tag_key_model)
        if (model is GradeFull) {
            onGradeClick?.invoke(model)
            return@OnClickListener
        }
        if (model !is ExpandableItemModel<*>)
            return@OnClickListener
        val position = items.indexOf(model)
        if (position == -1)
            return@OnClickListener
        //val position = it.getTag(R.string.tag_key_position) as? Int ?: return@OnClickListener

        if (model is GradesSubject || model is GradesSemester) {
            view.findViewById<View>(R.id.dropdownIcon)?.let { dropdownIcon ->
                ObjectAnimator.ofFloat(
                    dropdownIcon,
                    View.ROTATION,
                    if (model.state == STATE_CLOSED) 0f else 180f,
                    if (model.state == STATE_CLOSED) 180f else 0f
                ).setDuration(200).start();
            }
        }
        if (model is GradesSubject) {
            val preview = view.findViewById<View>(R.id.previewContainer)
            val summary = view.findViewById<View>(R.id.yearSummary)
            preview?.visibility = if (model.state == STATE_CLOSED) View.INVISIBLE else View.VISIBLE
            summary?.visibility = if (model.state == STATE_CLOSED) View.VISIBLE else View.INVISIBLE
        }

        if (model.state == STATE_CLOSED) {

            val subItems = if (model is GradesSemester && model.grades.isEmpty())
                listOf(GradesEmpty())
            else
                model.items

            model.state = STATE_OPENED
            items.addAll(position + 1, subItems.filterNotNull())
            notifyItemRangeInserted(position + 1, subItems.size)
            /*notifyItemRangeChanged(
                position + subItems.size,
                items.size - (position + subItems.size)
            )*/
            //notifyItemRangeChanged(position, items.size - position)

            if (model is GradesSubject) {
                // auto expand first semester
                if (model.semesters.isNotEmpty()) {
                    val semester = model.semesters.firstOrNull { it.grades.isNotEmpty() } ?: model.semesters.first()
                    val semesterIndex = model.semesters.indexOf(semester)
                    val grades = if (semester.grades.isEmpty())
                        listOf(GradesEmpty())
                    else
                        semester.grades
                    semester.state = STATE_OPENED
                    items.addAll(position + 2 + semesterIndex, grades)
                    notifyItemRangeInserted(position + 2 + semesterIndex, grades.size)
                }
            }
        }
        else {
            val start = position + 1
            var end: Int = items.size
            for (i in start until items.size) {
                val model1 = items[i]
                val level = if (model1 is GradesStats) 0 else (model1 as? ExpandableItemModel<*>)?.level ?: 3
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
                notifyItemRangeRemoved(start, end - start)
                //notifyItemRangeChanged(start, end - start)
                //notifyItemRangeChanged(position, items.size - position)
            }

            model.state = STATE_CLOSED
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder !is BindableViewHolder<*>)
            return

        val app = activity.applicationContext as App

        val viewType = when (holder) {
            is SubjectViewHolder -> ITEM_TYPE_SUBJECT
            is SemesterViewHolder -> ITEM_TYPE_SEMESTER
            is EmptyViewHolder -> ITEM_TYPE_EMPTY
            is GradeViewHolder -> ITEM_TYPE_GRADE
            is StatsViewHolder -> ITEM_TYPE_STATS
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
        holder.itemView.setTag(R.string.tag_key_view_type, viewType)
        holder.itemView.setTag(R.string.tag_key_position, position)
        holder.itemView.setTag(R.string.tag_key_model, item)

        when {
            holder is SubjectViewHolder && item is GradesSubject -> holder.onBind(activity, app, item, position)
            holder is SemesterViewHolder && item is GradesSemester -> holder.onBind(activity, app, item, position)
            holder is EmptyViewHolder && item is GradesEmpty -> holder.onBind(activity, app, item, position)
            holder is GradeViewHolder && item is GradeFull -> holder.onBind(activity, app, item, position)
            holder is StatsViewHolder && item is GradesStats -> holder.onBind(activity, app, item, position)
        }

        if (holder is SemesterViewHolder && item is GradesSemester) {
            holder.b.editButton.onClick {
                val subject = items.firstOrNull { it is GradesSubject && it.subjectId == item.subjectId } as? GradesSubject ?: return@onClick
                onGradesEditorClick?.invoke(subject, item)
            }
        }

        holder.itemView.setOnClickListener(onClickListener)
    }

    override fun getItemCount() = items.size
}
