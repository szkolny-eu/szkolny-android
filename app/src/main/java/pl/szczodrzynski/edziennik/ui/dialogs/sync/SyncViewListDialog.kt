/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-13.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.ext.hasFeature
import pl.szczodrzynski.edziennik.ext.resolveString
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesFragment

class SyncViewListDialog(
    activity: MainActivity,
    private val currentNavTarget: NavTarget,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog<FeatureType>(activity, onShowListener, onDismissListener) {

    override val TAG = "SyncViewListDialog"

    override fun getTitleRes() = R.string.dialog_sync_view_list_title
    override fun getPositiveButtonText() = R.string.ok
    override fun getNeutralButtonText() = R.string.sync_feature_all
    override fun getNegativeButtonText() = R.string.cancel

    @Suppress("USELESS_CAST")
    override fun getMultiChoiceItems() = FeatureType.values()
        .filter { it.nameRes != null && app.profile.hasFeature(it) }
        .associateBy { it.nameRes!!.resolveString(activity) as CharSequence }

    override fun getDefaultSelectedItems() = when (currentNavTarget) {
        NavTarget.HOME -> getMultiChoiceItems().values.toSet()
        NavTarget.MESSAGES -> when (MessagesFragment.pageSelection) {
            Message.TYPE_SENT -> setOf(FeatureType.MESSAGES_SENT)
            else -> setOf(FeatureType.MESSAGES_INBOX)
        }
        else -> currentNavTarget.featureType?.let { setOf(it) } ?: getMultiChoiceItems().values.toSet()
    }

    override suspend fun onShow() = Unit

    @Suppress("UNCHECKED_CAST")
    override suspend fun onPositiveClick(): Boolean {
        val selected = getMultiSelection()
        if (selected.isEmpty())
            return DISMISS

        if (activity is MainActivity)
            activity.swipeRefreshLayout.isRefreshing = true
        EdziennikTask.syncProfile(
            App.profileId,
            selected
        ).enqueue(activity)
        return DISMISS
    }

    override suspend fun onNeutralClick(): Boolean {
        if (activity is MainActivity)
            activity.swipeRefreshLayout.isRefreshing = true
        EdziennikTask.syncProfile(App.profileId).enqueue(activity)
        return DISMISS
    }
}
