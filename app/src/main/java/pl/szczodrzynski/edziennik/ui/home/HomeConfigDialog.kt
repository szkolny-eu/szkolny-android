/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-11.
 */

package pl.szczodrzynski.edziennik.ui.home

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog
import pl.szczodrzynski.edziennik.ui.home.HomeCard.Companion.CARD_EVENTS
import pl.szczodrzynski.edziennik.ui.home.HomeCard.Companion.CARD_GRADES
import pl.szczodrzynski.edziennik.ui.home.HomeCard.Companion.CARD_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.ui.home.HomeCard.Companion.CARD_TIMETABLE

class HomeConfigDialog(
    activity: AppCompatActivity,
    private val reloadOnDismiss: Boolean = true,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog(activity, onShowListener, onDismissListener) {

    override val TAG = "HomeConfigDialog"

    override fun getTitleRes() = R.string.home_configure_add_remove
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getMultiChoiceItems(): Map<CharSequence, Any> = mapOf(
        R.string.card_type_lucky_number to CARD_LUCKY_NUMBER,
        R.string.card_type_timetable to CARD_TIMETABLE,
        R.string.card_type_grades to CARD_GRADES,
        R.string.card_type_events to CARD_EVENTS,
    ).mapKeys { (resId, _) -> activity.getString(resId) }

    override fun getDefaultSelectedItems() =
        profileConfig.homeCards
            .filter { it.profileId == App.profileId }
            .map { it.cardId }
            .toSet()

    override suspend fun onShow() = Unit

    private val profileConfig by lazy { app.config.getFor(app.profileId).ui }
    private var configChanged = false

    override suspend fun onPositiveClick(): Boolean {
        val homeCards = profileConfig.homeCards.toMutableList()
        homeCards.removeAll { it.profileId == App.profileId }
        homeCards += getMultiSelection().mapNotNull {
            HomeCardModel(
                profileId = App.profileId,
                cardId = it as? Int ?: return@mapNotNull null
            )
        }
        profileConfig.homeCards = homeCards
        return DISMISS
    }

    override suspend fun onMultiSelectionChanged(items: Set<Any>) {
        configChanged = true
    }

    override fun onDismiss() {
        if (configChanged && reloadOnDismiss && activity is MainActivity)
            activity.reloadTarget()
    }
}
