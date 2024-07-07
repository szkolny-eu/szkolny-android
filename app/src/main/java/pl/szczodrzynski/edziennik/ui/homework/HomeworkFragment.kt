/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.homework

import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.CoroutineScope
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.databinding.BasePagerFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ui.base.fragment.PagerFragment
import pl.szczodrzynski.edziennik.ui.event.EventManualDialog
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem

class HomeworkFragment : PagerFragment<BasePagerFragmentBinding, MainActivity>(
    inflater = BasePagerFragmentBinding::inflate,
), CoroutineScope {

    override fun getFab() = R.string.add to CommunityMaterial.Icon3.cmd_plus
    override fun getMarkAsReadType() = MetadataType.HOMEWORK
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_add_event)
            .withDescription(R.string.menu_add_event_desc)
            .withIcon(SzkolnyFont.Icon.szf_calendar_plus_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                EventManualDialog(activity, App.profileId, defaultType = Event.TYPE_HOMEWORK).show()
            }
    )

    override fun getTabLayout() = b.tabLayout
    override fun getViewPager() = b.viewPager
    override suspend fun onCreatePages() = listOf(
        HomeworkListFragment().apply {
            arguments = Bundle("homeworkDate" to HomeworkDate.CURRENT)
        } to getString(R.string.homework_tab_current),
        HomeworkListFragment().apply {
            arguments = Bundle("homeworkDate" to HomeworkDate.PAST)
        } to getString(R.string.homework_tab_past)
    )

    override suspend fun onFabClick() {
        EventManualDialog(activity, App.profileId, defaultType = Event.TYPE_HOMEWORK).show()
    }
}
