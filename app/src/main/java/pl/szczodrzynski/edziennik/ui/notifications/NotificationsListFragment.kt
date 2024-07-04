/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-22.
 */

package pl.szczodrzynski.edziennik.ui.notifications

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.NotificationsListFragmentBinding
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import timber.log.Timber

class NotificationsListFragment : BaseFragment<NotificationsListFragmentBinding, MainActivity>(
    inflater = NotificationsListFragmentBinding::inflate,
) {

    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_remove_notifications)
            .withIcon(CommunityMaterial.Icon.cmd_delete_sweep_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                launch(Dispatchers.IO) {
                    app.db.notificationDao().clearAll()
                }
                Toast.makeText(
                    activity,
                    R.string.menu_remove_notifications_success,
                    Toast.LENGTH_SHORT
                ).show()
            }
    )

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        val adapter = NotificationsAdapter(activity) { notification ->
            val intent = Intent("android.intent.action.MAIN")
            notification.fillIntent(intent)

            Timber.d("notification with item " + notification.navTarget + " extras " + if (intent.extras == null) "null" else intent.extras!!.toString())
            if (notification.profileId != null && notification.profileId != -1 && notification.profileId != app.profile.id && context is Activity) {
                Toast.makeText(app, app.getString(R.string.toast_changing_profile), Toast.LENGTH_LONG).show()
            }
            app.sendBroadcast(intent)
        }

        app.db.notificationDao().getAll().observe(viewLifecycleOwner, Observer { items ->
            if (!isAdded) return@Observer

            // load & configure the adapter
            adapter.items = items
            if (items.isNotNullNorEmpty() && b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
                }
            }
            adapter.notifyDataSetChanged()

            // show/hide relevant views
            b.progressBar.isVisible = false
            if (items.isNullOrEmpty()) {
                b.list.isVisible = false
                b.noData.isVisible = true
            } else {
                b.list.isVisible = true
                b.noData.isVisible = false
            }
        })
    }
}
