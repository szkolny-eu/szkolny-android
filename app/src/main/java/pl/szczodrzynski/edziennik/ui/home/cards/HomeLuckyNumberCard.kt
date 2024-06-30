/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-24.
 */

package pl.szczodrzynski.edziennik.ui.home.cards

import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.plusAssign
import androidx.core.view.setMargins
import androidx.lifecycle.Observer
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.CardHomeLuckyNumberBinding
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ui.dialogs.settings.StudentNumberDialog
import pl.szczodrzynski.edziennik.ui.home.HomeCard
import pl.szczodrzynski.edziennik.ui.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.home.HomeFragment
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.colorAttr
import kotlin.coroutines.CoroutineContext

class HomeLuckyNumberCard(
        override val id: Int,
        val app: App,
        val activity: MainActivity,
        val fragment: HomeFragment,
        val profile: Profile
) : HomeCard, CoroutineScope {
    companion object {
        private const val TAG = "HomeLuckyNumberCard"
    }

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun bind(position: Int, holder: HomeCardAdapter.ViewHolder) { launch {
        holder.root.removeAllViews()
        val b = CardHomeLuckyNumberBinding.inflate(LayoutInflater.from(holder.root.context))
        b.root.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(8.dp)
        }
        holder.root += b.root

        val today = Date.getToday()
        val todayValue = today.value

        val tomorrow = Date.getToday().stepForward(0, 0, 1)
        val tomorrowValue = tomorrow.value

        val subTextRes = if (profile.studentNumber == -1)
            R.string.home_lucky_number_details_click_to_set
        else
            R.string.home_lucky_number_details
        b.subText.setText(subTextRes, profile.studentNumber)

        app.db.luckyNumberDao().getNearestFuture(profile.id, today).observe(fragment, Observer { luckyNumber ->
            val isYours = luckyNumber?.number == profile.studentNumber
            val res: Pair<Int, Array<out Any>> = when {
                luckyNumber == null -> R.string.home_lucky_number_no_info to emptyArray()
                luckyNumber.number == -1 -> R.string.home_lucky_number_no_number to emptyArray()
                else -> when (isYours) {
                    true -> when (luckyNumber.date.value) {
                        todayValue -> R.string.home_lucky_number_yours_today to emptyArray()
                        tomorrowValue -> R.string.home_lucky_number_yours_tomorrow to emptyArray()
                        else -> R.string.home_lucky_number_yours_later to arrayOf(luckyNumber.date.formattedString)
                    }
                    false -> when (luckyNumber.date.value) {
                        todayValue -> R.string.home_lucky_number_today to arrayOf(luckyNumber.number)
                        tomorrowValue -> R.string.home_lucky_number_tomorrow to arrayOf(luckyNumber.number)
                        else -> R.string.home_lucky_number_later to arrayOf(luckyNumber.date.formattedString, luckyNumber.number)
                    }
                }
            }

            val (titleRes, resArguments) = res

            b.title.setText(titleRes, *resArguments)

            val icon: IIcon = when {
                luckyNumber == null || luckyNumber.number == -1 -> CommunityMaterial.Icon.cmd_emoticon_sad_outline
                isYours -> CommunityMaterial.Icon.cmd_emoticon_cool_outline
                !isYours -> CommunityMaterial.Icon.cmd_emoticon_happy_outline
                else -> CommunityMaterial.Icon.cmd_emoticon_confused_outline
            }
            b.image.icon = IconicsDrawable(activity, icon).apply {
                sizeDp = 24
                colorAttr(activity, R.attr.colorOnPrimaryContainer)
            }
        })

        holder.root.onClick {
            StudentNumberDialog(activity, profile, onDismissListener = {
                app.profileSave(profile)
                val newSubTextRes = if (profile.studentNumber == -1)
                    R.string.home_lucky_number_details_click_to_set
                else
                    R.string.home_lucky_number_details
                b.subText.setText(newSubTextRes, profile.studentNumber)
            })
        }
    }}

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) = Unit
}
