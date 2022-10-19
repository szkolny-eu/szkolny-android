/*
 * Copyright (c) Kuba Szczodrzyński 2020-8-25.
 */

package pl.szczodrzynski.edziennik.ui.home.cards

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.plusAssign
import androidx.core.view.setMargins
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.CardHomeArchiveBinding
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.setMessage
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.home.HomeCard
import pl.szczodrzynski.edziennik.ui.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.home.HomeFragment
import kotlin.coroutines.CoroutineContext

class HomeArchiveCard(
        override val id: Int,
        val app: App,
        val activity: MainActivity,
        val fragment: HomeFragment,
        val profile: Profile
) : HomeCard, CoroutineScope {
    companion object {
        private const val TAG = "HomeArchiveCard"
    }

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun bind(position: Int, holder: HomeCardAdapter.ViewHolder) {
        holder.root.removeAllViews()
        val b = CardHomeArchiveBinding.inflate(LayoutInflater.from(holder.root.context))
        b.root.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(8.dp)
        }
        holder.root += b.root

        b.homeArchiveText.setText(
                R.string.home_archive_text,
                profile.studentSchoolYearStart,
                profile.studentSchoolYearStart + 1
        )

        b.homeArchiveClose.onClick {
            launch {
                val profile = profile.archiveId?.let {
                    withContext(Dispatchers.IO) {
                        app.db.profileDao().getNotArchivedOf(it)
                    }
                }
                if (profile == null) {
                    MaterialAlertDialogBuilder(activity)
                            .setTitle(R.string.home_archive_close_no_target_title)
                            .setMessage(R.string.home_archive_close_no_target_text, this@HomeArchiveCard.profile.name)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                activity.drawer.profileSelectionOpen()
                                activity.drawer.open()
                            }
                            .show()
                    return@launch
                }
                activity.navigate(profile = profile)
            }
        }

        holder.root.onClick {
            activity.navigate(navTarget = NavTarget.AGENDA)
        }
    }

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) = Unit
}
