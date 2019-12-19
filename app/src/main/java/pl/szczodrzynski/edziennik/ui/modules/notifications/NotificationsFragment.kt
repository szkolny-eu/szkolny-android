/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-22.
 */

package pl.szczodrzynski.edziennik.ui.modules.notifications

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentNotificationsBinding
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem


class NotificationsFragment : Fragment() {
    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentNotificationsBinding

    private val adapter by lazy {
        NotificationsAdapter(activity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false)
        // activity, context and profile is valid
        b = FragmentNotificationsBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_remove_notifications)
                        .withIcon(CommunityMaterial.Icon.cmd_delete_sweep_outline)
                        .withOnClickListener(View.OnClickListener {
                            activity.bottomSheet.close()
                            AsyncTask.execute { app.db.notificationDao().clearAll() }
                            Toast.makeText(activity, R.string.menu_remove_notifications_success, Toast.LENGTH_SHORT).show()
                        }))

        app.db.notificationDao()
                .getAll()
                .observe(this, Observer { notifications ->
                    if (app.profile == null || !isAdded) return@Observer

                    adapter.items = notifications
                    if (b.notificationsView.adapter == null) {
                        b.notificationsView.adapter = adapter
                        b.notificationsView.apply {
                            setHasFixedSize(true)
                            layoutManager = LinearLayoutManager(context)
                            addItemDecoration(SimpleDividerItemDecoration(context))
                        }
                    }
                    adapter.notifyDataSetChanged()

                    if (notifications != null && notifications.isNotEmpty()) {
                        b.notificationsView.visibility = View.VISIBLE
                        b.notificationsNoData.visibility = View.GONE
                    } else {
                        b.notificationsView.visibility = View.GONE
                        b.notificationsNoData.visibility = View.VISIBLE
                    }
                })
    }
}
