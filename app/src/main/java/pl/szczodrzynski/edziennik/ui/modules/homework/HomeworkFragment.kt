package pl.szczodrzynski.edziennik.ui.modules.homework

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.databinding.FragmentHomeworkBinding
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesFragment
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem

class HomeworkFragment : Fragment() {
    companion object {
        var pageSelection = 0
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentHomeworkBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false)
        // activity, context and profile is valid
        b = FragmentHomeworkBinding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                .withTitle(R.string.menu_add_event)
                .withDescription(R.string.menu_add_event_desc)
                .withIcon(SzkolnyFont.Icon.szf_calendar_plus_outline)
                .withOnClickListener(View.OnClickListener {
                    activity.bottomSheet.close()
                    EventManualDialog(activity, App.profileId, defaultType = Event.TYPE_HOMEWORK)
                }),
        BottomSheetSeparatorItem(true),
        BottomSheetPrimaryItem(true)
                .withTitle(R.string.menu_mark_as_read)
                .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                .withOnClickListener(View.OnClickListener {
                    activity.bottomSheet.close()
                    AsyncTask.execute { app.db.metadataDao().setAllSeen(App.profileId, Metadata.TYPE_HOMEWORK, true) }
                    Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show()
                }))

        b.viewPager.adapter = MessagesFragment.Adapter(childFragmentManager).also { adapter ->
            adapter.addFragment(HomeworkListFragment().also { fragment ->
                fragment.arguments = Bundle().also {  args ->
                    args.putInt("homeworkDate", HomeworkDate.CURRENT)
                }
            }, getString(R.string.homework_tab_current))

            adapter.addFragment(HomeworkListFragment().also { fragment ->
                fragment.arguments = Bundle().also {  args ->
                    args.putInt("homeworkDate", HomeworkDate.PAST)
                }
            }, getString(R.string.homework_tab_past))
        }

        b.viewPager.currentItem = pageSelection
        b.viewPager.clearOnPageChangeListeners()
        b.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (b.refreshLayout != null) {
                    b.refreshLayout.isEnabled = state == ViewPager.SCROLL_STATE_IDLE
                }
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                pageSelection = position
            }
        })

        b.tabLayout.setupWithViewPager(b.viewPager)

        activity.navView.bottomBar.fabEnable = true
        activity.navView.bottomBar.fabExtendedText = getString(R.string.add)
        activity.navView.bottomBar.fabIcon = CommunityMaterial.Icon2.cmd_plus
        activity.navView.setFabOnClickListener(View.OnClickListener {
            EventManualDialog(activity, App.profileId, defaultType = Event.TYPE_HOMEWORK)
        })

        activity.gainAttention()
        activity.gainAttentionFAB()
    }
}
