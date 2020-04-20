/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-22.
 */

package pl.szczodrzynski.edziennik.ui.modules.notifications

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.databinding.NotificationsListFragmentBinding
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import kotlin.coroutines.CoroutineContext

class NotificationsListFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "NotificationsListFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: NotificationsListFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = NotificationsListFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { startCoroutineTimer(100L) {
        if (!isAdded) return@startCoroutineTimer

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_remove_notifications)
                        .withIcon(CommunityMaterial.Icon.cmd_delete_sweep_outline)
                        .withOnClickListener(View.OnClickListener {
                            activity.bottomSheet.close()
                            AsyncTask.execute { app.db.notificationDao().clearAll() }
                            Toast.makeText(activity, R.string.menu_remove_notifications_success, Toast.LENGTH_SHORT).show()
                        }))

        val adapter = NotificationsAdapter(activity) { notification ->
            val intent = Intent("android.intent.action.MAIN")
            notification.fillIntent(intent)

            Utils.d(TAG, "notification with item " + notification.viewId + " extras " + if (intent.extras == null) "null" else intent.extras!!.toString())
            if (notification.profileId != null && notification.profileId != -1 && notification.profileId != app.profile.id && context is Activity) {
                Toast.makeText(app, app.getString(R.string.toast_changing_profile), Toast.LENGTH_LONG).show()
            }
            app.sendBroadcast(intent)
        }

        app.db.notificationDao().getAll().observe(this@NotificationsListFragment, Observer { items ->
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
    }}
}
