package pl.szczodrzynski.edziennik.ui.modules.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.FragmentMessagesBinding
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.utils.Themes
import java.util.*

class MessagesFragment : Fragment() {
    companion object {
        var pageSelection = 0
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentMessagesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false)
        // activity, context and profile is valid
        b = FragmentMessagesBinding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        val messageId = arguments?.getLong("messageId", -1L) ?: -1L
        if (messageId != -1L) {
            val args = Bundle()
            args.putLong("messageId", messageId)
            arguments!!.remove("messageId")
            activity.loadTarget(MainActivity.TARGET_MESSAGES_DETAILS, args)
            return
        }

        b.viewPager.adapter = Adapter(childFragmentManager).also { adapter ->

            adapter.addFragment(MessagesListFragment().also { fragment ->
                fragment.arguments = Bundle().also { args ->
                    args.putInt("messageType", Message.TYPE_RECEIVED)
                }
            }, getString(R.string.menu_messages_inbox))

            adapter.addFragment(MessagesListFragment().also { fragment ->
                fragment.arguments = Bundle().also { args ->
                    args.putInt("messageType", Message.TYPE_SENT)
                }
            }, getString(R.string.menu_messages_sent))

        }

        b.viewPager.currentItem = pageSelection
        b.viewPager.clearOnPageChangeListeners()
        b.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                pageSelection = position
            }
        })

        b.tabLayout.setupWithViewPager(b.viewPager)
    }

    internal class Adapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {
        private val mFragmentList = ArrayList<Fragment>()
        private val mFragmentTitleList = ArrayList<String>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitleList[position]
        }
    }
}
