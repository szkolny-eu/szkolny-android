/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.grades

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NO_GRADE
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.grades.models.*
import pl.szczodrzynski.edziennik.ui.grades.viewholder.*
import kotlin.coroutines.CoroutineContext

class GradesAdapter(
        val activity: AppCompatActivity,
        val showNotes: Boolean = true,
        var onGradeClick: ((item: GradeFull) -> Unit)? = null,
        var onGradesEditorClick: ((subject: GradesSubject, semester: GradesSemester) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "GradesAdapter"
        private const val ITEM_TYPE_SUBJECT = 0
        private const val ITEM_TYPE_SEMESTER = 1
        private const val ITEM_TYPE_EMPTY = 2
        private const val ITEM_TYPE_GRADE = 3
        private const val ITEM_TYPE_STATS = 4
        private const val ITEM_TYPE_UNKNOWN_SUBJECT = 5
        const val STATE_CLOSED = 0
        const val STATE_OPENED = 1
    }

    private val app = activity.applicationContext as App
    private val manager
        get() = app.gradesManager

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = mutableListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_SUBJECT -> SubjectViewHolder(inflater, parent)
            ITEM_TYPE_SEMESTER -> SemesterViewHolder(inflater, parent)
            ITEM_TYPE_EMPTY -> EmptyViewHolder(inflater, parent)
            ITEM_TYPE_GRADE -> GradeViewHolder(inflater, parent)
            ITEM_TYPE_STATS -> StatsViewHolder(inflater, parent)
            ITEM_TYPE_UNKNOWN_SUBJECT -> UnknownSubjectViewHolder(inflater, parent)
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
            is GradesUnknownSubject -> ITEM_TYPE_UNKNOWN_SUBJECT
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
        expandModel(model, view)
    }

    fun expandModel(model: ExpandableItemModel<*>?, view: View?, notifyAdapter: Boolean = true) {
        model ?: return
        var position = items.indexOf(model)
        if (position == -1)
            return

        if (model is GradesSubject || model is GradesSemester) {
            view?.findViewById<View>(R.id.dropdownIcon)?.let { dropdownIcon ->
                ObjectAnimator.ofFloat(
                        dropdownIcon,
                        View.ROTATION,
                        if (model.state == STATE_CLOSED) 0f else 180f,
                        if (model.state == STATE_CLOSED) 180f else 0f
                ).setDuration(200).start();
            }
        }
        if (model is GradesSubject) {
            // hide the preview, show summary
            val preview = view?.findViewById<View>(R.id.previewContainer)
            val summary = view?.findViewById<View>(R.id.yearSummary)
            preview?.visibility = if (model.state == STATE_CLOSED) View.INVISIBLE else View.VISIBLE
            summary?.visibility = if (model.state == STATE_CLOSED) View.VISIBLE else View.INVISIBLE

            // expanding a subject - mark proposed & final grade as seen
            var unseenChanged = false
            if (model.proposedGrade?.seen == false) {
                manager.markAsSeen(model.proposedGrade!!)
                unseenChanged = true
            }
            if (model.finalGrade?.seen == false) {
                manager.markAsSeen(model.finalGrade!!)
                unseenChanged = true
            }
            // remove the override flag
            model.hasUnseen = false

            if (unseenChanged) {
                // check if the unseen status has changed
                if (!model.hasUnseen) {
                    notifyItemChanged(model)
                }
            }
        }

        if (model.state == STATE_CLOSED) {

            val subItems = when {
                model is GradesSubject && manager.isUniversity -> listOf()
                model is GradesSemester && model.grades.isEmpty() ->
                    listOf(GradesEmpty())
                model is GradesSemester && manager.hideImproved ->
                    model.items.filter { !it.seen || !it.isImproved }
                else -> model.items
            }

            if (model is GradesSubject && model.isUnknown) {
                position++
                items.add(position, GradesUnknownSubject())
                if (notifyAdapter) notifyItemInserted(position)
            }

            model.state = STATE_OPENED
            if (subItems.isNotEmpty()) {
                position++
                items.addAll(position, subItems.filterNotNull())
                if (notifyAdapter) notifyItemRangeInserted(position, subItems.size)
            }

            if (model is GradesSubject) {
                // auto expand first semester
                if (model.semesters.isNotEmpty()) {
                    val semester = model.semesters.firstOrNull { it.grades.isNotEmpty() } ?: model.semesters.first()
                    val semesterIndex = model.semesters.indexOf(semester)

                    val grades = when {
                        semester.grades.isEmpty() ->
                            listOf(GradesEmpty())
                        manager.hideImproved ->
                            semester.grades.filter { !it.seen || !it.isImproved }
                        else -> semester.grades
                    }

                    position++
                    semester.state = STATE_OPENED
                    items.addAll(position + semesterIndex, grades)
                    if (notifyAdapter) notifyItemRangeInserted(position + semesterIndex, grades.size)
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
            is SubjectViewHolder -> ITEM_TYPE_SUBJECT
            is SemesterViewHolder -> ITEM_TYPE_SEMESTER
            is EmptyViewHolder -> ITEM_TYPE_EMPTY
            is GradeViewHolder -> ITEM_TYPE_GRADE
            is StatsViewHolder -> ITEM_TYPE_STATS
            is UnknownSubjectViewHolder -> ITEM_TYPE_UNKNOWN_SUBJECT
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
        holder.itemView.setTag(R.string.tag_key_view_type, viewType)
        holder.itemView.setTag(R.string.tag_key_position, position)
        holder.itemView.setTag(R.string.tag_key_model, item)

        when {
            holder is SubjectViewHolder && item is GradesSubject -> holder.onBind(activity, app, item, position, this)
            holder is SemesterViewHolder && item is GradesSemester -> holder.onBind(activity, app, item, position, this)
            holder is EmptyViewHolder && item is GradesEmpty -> holder.onBind(activity, app, item, position, this)
            holder is GradeViewHolder && item is GradeFull -> holder.onBind(activity, app, item, position, this)
            holder is StatsViewHolder && item is GradesStats -> holder.onBind(activity, app, item, position, this)
            holder is UnknownSubjectViewHolder && item is GradesUnknownSubject -> holder.onBind(activity, app, item, position, this)
        }

        if (holder is SemesterViewHolder && item is GradesSemester) {
            holder.b.editButton.onClick {
                val subject = items.firstOrNull { it is GradesSubject && it.subjectId == item.subjectId } as? GradesSubject ?: return@onClick
                onGradesEditorClick?.invoke(subject, item)
            }
        }

        if (item !is GradeFull || (onGradeClick != null && item.type != TYPE_NO_GRADE)) {
            holder.itemView.setOnClickListener(onClickListener)
            holder.itemView.isEnabled = true
        } else {
            holder.itemView.setOnClickListener(null)
            holder.itemView.isEnabled = false
        }
    }

    fun notifyItemChanged(model: Any) {
        startCoroutineTimer(1000L, 0L) {
            val index = items.indexOf(model)
            if (index != -1)
                notifyItemChanged(index)
        }
    }

    fun removeItem(model: Any) {
        startCoroutineTimer(2000L, 0L) {
            val index = items.indexOf(model)
            if (index != -1) {
                items.removeAt(index)
                notifyItemRemoved(index)
            }
        }
    }

    override fun getItemCount() = items.size
}
