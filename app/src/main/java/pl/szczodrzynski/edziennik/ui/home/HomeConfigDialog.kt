/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-11.
 */

package pl.szczodrzynski.edziennik.ui.home

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog
import pl.szczodrzynski.edziennik.ui.home.HomeCard.Companion.CARD_EVENTS
import pl.szczodrzynski.edziennik.ui.home.HomeCard.Companion.CARD_GRADES
import pl.szczodrzynski.edziennik.ui.home.HomeCard.Companion.CARD_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.ui.home.HomeCard.Companion.CARD_NOTES
import pl.szczodrzynski.edziennik.ui.home.HomeCard.Companion.CARD_TIMETABLE

class HomeConfigDialog(
    activity: AppCompatActivity,
    private val reloadOnDismiss: Boolean = true,
) : BaseDialog<Any>(activity) {

    override fun getTitleRes() = R.string.home_configure_add_remove
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getMultiChoiceItems(): Map<CharSequence, Any> = mapOf(
        R.string.card_type_lucky_number to CARD_LUCKY_NUMBER,
        R.string.card_type_timetable to CARD_TIMETABLE,
        R.string.card_type_grades to CARD_GRADES,
        R.string.card_type_events to CARD_EVENTS,
        R.string.card_type_notes to CARD_NOTES,
    ).mapKeys { (resId, _) -> activity.getString(resId) }

    override fun getDefaultSelectedItems() =
        app.profile.config.ui.homeCards
            .filter { it.profileId == App.profileId }
            .map { it.cardId }
            .toSet()

    private var configChanged = false

    override suspend fun onPositiveClick(): Boolean {
        val homeCards = app.profile.config.ui.homeCards.toMutableList()
        homeCards.removeAll { it.profileId == App.profileId }
        homeCards += getMultiSelection().mapNotNull {
            HomeCardModel(
                profileId = App.profileId,
                cardId = it as? Int ?: return@mapNotNull null
            )
        }
        app.profile.config.ui.homeCards = homeCards
        return DISMISS
    }

    override suspend fun onMultiSelectionChanged(item: Any, isChecked: Boolean) {
        configChanged = true
    }

    override suspend fun onDismiss() {
        if (configChanged && reloadOnDismiss && activity is MainActivity)
            activity.reloadTarget()
    }
}
