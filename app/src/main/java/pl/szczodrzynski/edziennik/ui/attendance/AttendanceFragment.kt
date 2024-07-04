/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-30.
 */

package pl.szczodrzynski.edziennik.ui.attendance

import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.databinding.BasePagerFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ui.base.fragment.PagerFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.AttendanceConfigDialog
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem

class AttendanceFragment : PagerFragment<BasePagerFragmentBinding, MainActivity>(
    inflater = BasePagerFragmentBinding::inflate,
) {
    companion object {
        const val VIEW_SUMMARY = 0
        const val VIEW_DAYS = 1
        const val VIEW_MONTHS = 2
        const val VIEW_TYPES = 3
        const val VIEW_LIST = 4
    }

    override var savedPageSelection
        get() = app.profile.config.attendance.attendancePageSelection
        set(value) {
            app.profile.config.attendance.attendancePageSelection = value
        }

    override fun getMarkAsReadType() = MetadataType.ATTENDANCE
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_attendance_config)
            .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                AttendanceConfigDialog(activity, true, null, null).show()
            },
    )

    override fun getTabLayout() = b.tabLayout
    override fun getViewPager() = b.viewPager
    override suspend fun onCreatePages() = listOf(
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
}
