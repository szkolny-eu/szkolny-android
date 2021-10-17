/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-29
 */

package pl.szczodrzynski.edziennik.ui.home.cards

import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.view.plusAssign
import androidx.core.view.setMargins
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Subject
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.databinding.CardHomeGradesBinding
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ui.grades.GradeView
import pl.szczodrzynski.edziennik.ui.home.HomeCard
import pl.szczodrzynski.edziennik.ui.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.home.HomeFragment
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.ItemGradesSubjectModel
import kotlin.coroutines.CoroutineContext

class HomeGradesCard(
        override val id: Int,
        val app: App,
        val activity: MainActivity,
        val fragment: HomeFragment,
        val profile: Profile
) : HomeCard, CoroutineScope {

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var b: CardHomeGradesBinding

    private val grades = mutableListOf<GradeFull>()
    private val subjects = mutableListOf<ItemGradesSubjectModel>()

    override fun bind(position: Int, holder: HomeCardAdapter.ViewHolder) {
        holder.root.removeAllViews()
        b = CardHomeGradesBinding.inflate(LayoutInflater.from(holder.root.context))
        b.root.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(8.dp)
        }
        holder.root += b.root

        val sevenDaysAgo = Date.getToday().stepForward(0, 0, -7)

        app.db.gradeDao().getAllFromDate(profile.id, sevenDaysAgo).observe(fragment, Observer {
            grades.apply {
                clear()
                addAll(it)
            }
            update()
        })

        b.root.setOnClickListener {
            activity.loadTarget(MainActivity.DRAWER_ITEM_GRADES)
        }
    }

    private fun update() {
        subjects.clear()

        grades.forEach { grade ->
            val model = ItemGradesSubjectModel.searchModelBySubjectId(subjects, grade.subjectId)
                    ?: run {
                        subjects.add(ItemGradesSubjectModel(
                                profile,
                                Subject(profile.id, grade.subjectId, grade.subjectLongName, grade.subjectShortName),
                                mutableListOf(),
                                mutableListOf()
                        ))
                        ItemGradesSubjectModel.searchModelBySubjectId(subjects, grade.subjectId)
                    }

            model?.grades1?.add(grade)
        }

        b.gradeList.removeAllViews()

        val textLayoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setMargins(0, 0, 5.dp, 0)
        }

        val linearLayoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setMargins(2.dp, 0, 2.dp, 5.dp)
        }

        subjects.forEach { subject ->
            val gradeItem = LinearLayout(b.gradeList.context)
            gradeItem.orientation = HORIZONTAL

            var totalWidth = 0
            val maxWidth = (app.resources.displayMetrics.widthPixels -
                    Utils.dpToPx((if (app.config.ui.miniMenuVisible) 72 else 0) /*miniDrawer size*/ +
                            24 + 24 /*left and right offsets*/ +
                            16 /*ellipsize width*/)) / 1.5f

            subject.grades1.onEach { grade ->
                val gradeName = GradeView(
                        gradeItem.context,
                        grade,
                        app.gradesManager,
                        periodGradesTextual = true
                )

                totalWidth += gradeName.measuredWidth + 5.dp

                if (totalWidth >= maxWidth) {
                    val ellipsisText = TextView(gradeItem.context).apply {
                        text = app.getString(R.string.ellipsis)

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            setTextAppearance(context, R.style.NavView_TextView)
                        } else {
                            setTextAppearance(R.style.NavView_TextView)
                        }

                        setTypeface(null, Typeface.BOLD)
                        setPadding(0, 0, 0, 0)
                    }

                    gradeItem.addView(ellipsisText, textLayoutParams)
                    return@forEach
                } else {
                    gradeItem.addView(gradeName, textLayoutParams)
                }
            }

            val subjectName = TextView(gradeItem.context).apply {
                text = app.getString(R.string.grade_subject_format, subject.subject?.longName
                        ?: "")
                ellipsize = TextUtils.TruncateAt.END
                setSingleLine()
                measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            gradeItem.addView(subjectName, textLayoutParams)
            b.gradeList.addView(gradeItem, linearLayoutParams)
        }

        b.noData.visibility = if (subjects.isEmpty()) VISIBLE else GONE
        b.gradeList.visibility = if (subjects.isEmpty()) GONE else VISIBLE
    }

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) = Unit
}
