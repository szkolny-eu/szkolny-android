/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.ui.modules.homework.list

import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull
import pl.szczodrzynski.edziennik.data.db.modules.metadata.MetadataDao
import pl.szczodrzynski.edziennik.databinding.RowHomeworkItemBinding
import pl.szczodrzynski.edziennik.utils.Utils.bs
import pl.szczodrzynski.edziennik.utils.models.Date
import javax.inject.Inject
import kotlin.math.abs

class HomeworkListAdapter @Inject constructor(
        private val metadataDao: MetadataDao
) : RecyclerView.Adapter<HomeworkListAdapter.ViewHolder>() {

    val homeworkList: MutableList<EventFull> = mutableListOf()
    lateinit var onItemEditClick: (homework: EventFull) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val b: RowHomeworkItemBinding = DataBindingUtil.inflate(inflater, R.layout.row_homework_item, parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val homework = homeworkList[position]

        holder.apply {
            val diffDaysString = dateDiffString(Date.diffDays(homework.eventDate, Date.getToday()))

            b.homeworkItemHomeworkDate.text = getString(R.string.date_relative_format, homework.eventDate.formattedString, diffDaysString)
            b.homeworkItemTopic.text = homework.topic
            b.homeworkItemSubjectTeacher.text = getString(R.string.homework_subject_teacher_format, bs(homework.subjectLongName), bs(homework.teacherFullName))
            b.homeworkItemTeamDate.text = getString(R.string.homework_team_date_format, bs(homework.teamName), Date.fromMillis(homework.addedDate).formattedStringShort)

            when {
                !homework.seen -> {
                    b.homeworkItemTopic.apply {
                        background = getDrawable(R.drawable.bg_rounded_8dp)
                        background.colorFilter = PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY)
                    }

                    homework.seen = true
                    AsyncTask.execute {
                        metadataDao.setSeen(App.profileId, homework, true)
                    }
                }
                else -> b.homeworkItemTopic.background = null
            }

            b.homeworkItemEdit.apply {
                visibility = if (homework.addedManually) VISIBLE else GONE
                setOnClickListener { onItemEditClick(homework) }
            }

            b.homeworkItemSharedBy.apply {
                when {
                    homework.sharedBy == null -> visibility = GONE
                    homework.sharedByName != null -> text = getString(R.string.event_shared_by_format,
                            when (homework.sharedBy == "self") {
                                true -> getString(R.string.event_shared_by_self)
                                else -> homework.sharedByName
                            })
                }
            }
        }
    }

    override fun getItemCount() = homeworkList.size

    class ViewHolder(val b: RowHomeworkItemBinding) : RecyclerView.ViewHolder(b.root) {
        fun getString(resId: Int): String = itemView.context.getString(resId)
        fun getString(resId: Int, vararg formatArgs: Any): String = itemView.context.getString(resId, *formatArgs)
        fun getDrawable(resId: Int): Drawable? = ContextCompat.getDrawable(itemView.context, resId)

        val resources: Resources get() = itemView.context.resources

        fun dateDiffString(diff: Int): String {
            return when {
                diff > 0 -> when (diff) {
                    1 -> getString(R.string.tomorrow)
                    2 -> getString(R.string.the_day_after)
                    else -> resources.getQuantityString(R.plurals.time_till_days, abs(diff), abs(diff))
                }
                diff < 0 -> when (diff) {
                    -1 -> getString(R.string.yesterday)
                    -2 -> getString(R.string.the_day_before)
                    else -> getString(R.string.ago_format, resources.getQuantityString(R.plurals.time_till_days, abs(diff), abs(diff)))
                }
                else -> getString(R.string.today)
            }
        }
    }
}
