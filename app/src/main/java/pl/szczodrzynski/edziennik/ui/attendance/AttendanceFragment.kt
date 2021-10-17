/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-30.
 */

package pl.szczodrzynski.edziennik.ui.attendance

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.databinding.AttendanceFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.lazypager.FragmentLazyPagerAdapter
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AttendanceConfigDialog
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import kotlin.coroutines.CoroutineContext

class AttendanceFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "AttendanceFragment"
        const val VIEW_SUMMARY = 0
        const val VIEW_DAYS = 1
        const val VIEW_MONTHS = 2
        const val VIEW_TYPES = 3
        const val VIEW_LIST = 4
        var pageSelection = 1
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: AttendanceFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = AttendanceFragmentBinding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_attendance_config)
                        .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
                        .withOnClickListener(View.OnClickListener {
                            activity.bottomSheet.close()
                            AttendanceConfigDialog(activity, true, null, null)
                        }),
                BottomSheetSeparatorItem(true),
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                        .withOnClickListener(View.OnClickListener {
                            activity.bottomSheet.close()
                            AsyncTask.execute { App.db.metadataDao().setAllSeen(App.profileId, Metadata.TYPE_ATTENDANCE, true) }
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show()
                        })
        )
        activity.gainAttention()

        if (pageSelection == 1)
            pageSelection = app.config.forProfile().attendance.attendancePageSelection

        val pagerAdapter = FragmentLazyPagerAdapter(
                fragmentManager ?: return,
                b.refreshLayout,
                listOf(
                        AttendanceSummaryFragment() to getString(R.string.attendance_tab_summary),

                        AttendanceListFragment().apply {
                            arguments = Bundle("viewType" to VIEW_DAYS)
                        } to getString(R.string.attendance_tab_days),

                        AttendanceListFragment().apply {
                            arguments = Bundle("viewType" to VIEW_MONTHS)
                        } to getString(R.string.attendance_tab_months),

                        AttendanceListFragment().apply {
                            arguments = Bundle("viewType" to VIEW_TYPES)
                        } to getString(R.string.attendance_tab_types),

                        AttendanceListFragment().apply {
                            arguments = Bundle("viewType" to VIEW_LIST)
                        } to getString(R.string.attendance_tab_list)
                )
        )
        b.viewPager.apply {
            offscreenPageLimit = 1
            adapter = pagerAdapter
            currentItem = pageSelection
            addOnPageSelectedListener {
                pageSelection = it
                app.config.forProfile().attendance.attendancePageSelection = it
            }
            b.tabLayout.setupWithViewPager(this)
        }
    }
}
