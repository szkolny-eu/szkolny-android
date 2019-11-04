package pl.szczodrzynski.edziennik.ui.modules.homework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentHomeworkBinding
import pl.szczodrzynski.edziennik.ui.base.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.modules.homework.list.HomeworkListFragment
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesFragment
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem

class HomeworkFragment : BaseFragment<HomeworkPresenter>(), HomeworkView {
    companion object {
        var pageSelection = 0
    }

    override lateinit var app: App

    private lateinit var activity: MainActivity
    private lateinit var b: FragmentHomeworkBinding

    override val presenter: HomeworkPresenter = HomeworkPresenter()

    override val markAsReadSuccessString: String
        get() = getString(R.string.main_menu_mark_as_read_success)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.onAttachView(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity)
        app = activity.application as App

        context!!.theme.applyStyle(Themes.appTheme, true)
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false)
        b = FragmentHomeworkBinding.inflate(inflater)
        return b.root
    }

    override fun initView() {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        b.refreshLayout.setParent(activity.swipeRefreshLayout)

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_add_event)
                        .withDescription(R.string.menu_add_event_desc)
                        .withIcon(CommunityMaterial.Icon.cmd_calendar_plus)
                        .withOnClickListener(View.OnClickListener { presenter.onAddEventClick() }),
                BottomSheetSeparatorItem(true),
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check)
                        .withOnClickListener(View.OnClickListener { presenter.onMarkAsReadClick() }))

        b.viewPager.adapter = MessagesFragment.Adapter(childFragmentManager).also { adapter ->
            adapter.addFragment(HomeworkListFragment.newInstance(HomeworkDate.CURRENT), getString(R.string.homework_tab_current))
            adapter.addFragment(HomeworkListFragment.newInstance(HomeworkDate.PAST), getString(R.string.homework_tab_past))
        }

        b.viewPager.currentItem = pageSelection
        b.viewPager.clearOnPageChangeListeners()
        b.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) = presenter.onPageSelected(position)
        })

        b.tabLayout.setupWithViewPager(b.viewPager)

        activity.navView.bottomBar.fabEnable = true
        activity.navView.bottomBar.fabExtendedText = getString(R.string.add)
        activity.navView.bottomBar.fabIcon = CommunityMaterial.Icon2.cmd_plus
        activity.navView.setFabOnClickListener(View.OnClickListener { presenter.onHomeworkAddFabClick() })

        activity.gainAttention()
        activity.gainAttentionFAB()
    }

    override fun setPageSelection(position: Int) {
        pageSelection = position
    }

    override fun closeBottomSheet() {
        activity.bottomSheet.close()
    }

    override fun showAddHomeworkDialog() {
        EventManualDialog(activity).show(app, null, null, null, EventManualDialog.DIALOG_HOMEWORK)
    }
}
