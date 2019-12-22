/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-24.
 */

package pl.szczodrzynski.edziennik.ui.modules.home.cards

import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.plusAssign
import androidx.core.view.setMargins
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.databinding.CardHomeLuckyNumberBinding
import pl.szczodrzynski.edziennik.ui.dialogs.home.StudentNumberDialog
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCard
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment
import pl.szczodrzynski.edziennik.utils.models.Date
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

        val subTextRes = if (profile.studentNumber == -1)
            R.string.home_lucky_number_details_click_to_set
        else
            R.string.home_lucky_number_details
        b.subText.setText(subTextRes, profile.name ?: "", profile.studentNumber)

        app.db.luckyNumberDao().getNearestFuture(App.profileId, todayValue).observe(fragment, Observer { luckyNumber ->
            val isYours = luckyNumber?.number == profile.studentNumber
            val titleRes = when {
                luckyNumber == null -> R.string.home_lucky_number_no_info
                luckyNumber.number == -1 -> R.string.home_lucky_number_no_number
                else -> when (isYours) {
                    true -> when (luckyNumber.date.value) {
                        todayValue -> R.string.home_lucky_number_yours_today
                        todayValue + 1 -> R.string.home_lucky_number_yours_tomorrow
                        else -> R.string.home_lucky_number_yours_later
                    }
                    false -> when (luckyNumber.date.value) {
                        todayValue -> R.string.home_lucky_number_today
                        todayValue + 1 -> R.string.home_lucky_number_tomorrow
                        else -> R.string.home_lucky_number_later
                    }
                }
            }

            b.title.setText(
                    titleRes,
                    luckyNumber?.number ?: 0,
                    luckyNumber?.date?.formattedString ?: ""
            )

            val drawableRes = when {
                luckyNumber == null || luckyNumber.number == -1 -> R.drawable.emoji_sad
                isYours -> R.drawable.emoji_glasses
                !isYours -> R.drawable.emoji_smiling
                else -> R.drawable.emoji_no_face
            }
            b.image.setImageResource(drawableRes)
        })

        holder.root.onClick {
            StudentNumberDialog(activity, profile, onDismissListener = {
                app.profileSaveAsync(profile)
                val newSubTextRes = if (profile.studentNumber == -1)
                    R.string.home_lucky_number_details_click_to_set
                else
                    R.string.home_lucky_number_details
                b.subText.setText(newSubTextRes, profile.name ?: "", profile.studentNumber)
            })
        }
    }}

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) = Unit
}
