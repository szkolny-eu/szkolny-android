/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-29
 */

package pl.szczodrzynski.edziennik.ui.modules.home.cards

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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
import androidx.core.graphics.ColorUtils
import androidx.core.view.plusAssign
import androidx.core.view.setMargins
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.*
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeFull
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.databinding.CardHomeGradesBinding
import pl.szczodrzynski.edziennik.dp
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCard
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragmentV2
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.ItemGradesSubjectModel
import kotlin.coroutines.CoroutineContext

class HomeGradesCard(
        val id: Int,
        val app: App,
        val activity: MainActivity,
        val fragment: HomeFragmentV2,
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

        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000

        app.db.gradeDao().getAllFromDate(profile.id, profile.currentSemester, sevenDaysAgo).observe(fragment, Observer {
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

        val textLayoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        textLayoutParams.setMargins(0, 0, 5.dp, 0)

        val linearLayoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        linearLayoutParams.setMargins(8.dp, 0, 8.dp, 5.dp)

        subjects.forEach { subject ->
            val gradeItem = LinearLayout(b.gradeList.context)
            gradeItem.orientation = HORIZONTAL

            var totalWidth = 0
            val maxWidth = (app.resources.displayMetrics.widthPixels -
                    Utils.dpToPx((if (app.appConfig.miniDrawerVisible) 72 else 0) /*miniDrawer size*/ +
                            24 + 24 /*left and right offsets*/ +
                            16 /*ellipsize width*/)) / 1.5f

            subject.grades1.onEach { grade ->
                val gradeColor = when (app.profile.gradeColorMode) {
                    Profile.COLOR_MODE_DEFAULT -> grade.color
                    else -> Colors.gradeToColor(grade)
                }

                val gradeName = TextView(gradeItem.context).apply {
                    text = when (grade.type) {
                        TYPE_SEMESTER1_PROPOSED, TYPE_SEMESTER2_PROPOSED -> app.getString(R.string.grade_semester_proposed_format, grade.name)
                        TYPE_SEMESTER1_FINAL, TYPE_SEMESTER2_FINAL -> app.getString(R.string.grade_semester_final_format, grade.name)
                        TYPE_YEAR_PROPOSED -> app.getString(R.string.grade_year_proposed_format, grade.name)
                        TYPE_YEAR_FINAL -> app.getString(R.string.grade_year_final_format, grade.name)
                        else -> grade.name
                    }

                    setTextColor(when (ColorUtils.calculateLuminance(gradeColor) > 0.25) {
                        true -> 0xff000000
                        else -> 0xffffffff
                    }.toInt())

                    setTypeface(null, Typeface.BOLD)
                    setBackgroundResource(R.drawable.bg_rounded_4dp)
                    background.colorFilter = PorterDuffColorFilter(gradeColor, PorterDuff.Mode.MULTIPLY)
                    setPadding(5.dp, 0, 5.dp, 0)

                    measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }

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
