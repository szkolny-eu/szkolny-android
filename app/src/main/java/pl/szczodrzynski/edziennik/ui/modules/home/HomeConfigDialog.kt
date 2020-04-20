/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-11.
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCard.Companion.CARD_EVENTS
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCard.Companion.CARD_GRADES
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCard.Companion.CARD_LUCKY_NUMBER
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCard.Companion.CARD_TIMETABLE
import kotlin.collections.set

class HomeConfigDialog(
        val activity: AppCompatActivity,
        private val reloadOnDismiss: Boolean = true,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        const val TAG = "HomeConfigDialog"
    }

    private val app by lazy { activity.application as App }
    private val profileConfig by lazy { app.config.getFor(app.profileId).ui }

    private lateinit var dialog: AlertDialog

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)

        val ids = listOf(
                CARD_LUCKY_NUMBER,
                CARD_TIMETABLE,
                CARD_GRADES,
                CARD_EVENTS
        )
        val items = listOf(
                app.getString(R.string.card_type_lucky_number),
                app.getString(R.string.card_type_timetable),
                app.getString(R.string.card_type_grades),
                app.getString(R.string.card_type_events)
        )
        val checkedItems = ids.map { it to false }.toMap().toMutableMap()

        val profileId = App.profileId
        val homeCards = profileConfig.homeCards
                .filter { it.profileId == profileId }
                .toMutableList()

        homeCards.forEach {
            checkedItems[it.cardId] = true
        }

        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.home_configure_add_remove)
                .setMultiChoiceItems(items.toTypedArray(), checkedItems.values.toBooleanArray()) { _, which, isChecked ->
                    if (isChecked) {
                        homeCards += HomeCardModel(profileId, ids[which])
                    }
                    else {
                        homeCards.removeAll { it.profileId == profileId && it.cardId == ids[which] }
                    }
                }
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener {
                    profileConfig.homeCards = homeCards
                    onDismissListener?.invoke(TAG)
                    if (reloadOnDismiss) (activity as? MainActivity)?.reloadTarget()
                }
                .show()
    }}
}
