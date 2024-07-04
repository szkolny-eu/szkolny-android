package pl.szczodrzynski.edziennik.ui.messages.list

import android.os.Bundle
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.databinding.MessagesFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.addOnPageSelectedListener
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.base.lazypager.FragmentLazyPagerAdapter
import pl.szczodrzynski.edziennik.ui.dialogs.settings.MessagesConfigDialog
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem

class MessagesFragment : BaseFragment<MessagesFragmentBinding, MainActivity>(
    inflater = MessagesFragmentBinding::inflate,
) {
    companion object {
        var pageSelection = 0
    }

    override fun getFab() = R.string.compose to CommunityMaterial.Icon3.cmd_pencil_outline
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_messages_config)
            .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                MessagesConfigDialog(activity, false, null, null).show()
            }
    )

    override suspend fun onViewCreated(savedInstanceState: Bundle?) {
        val messageId = arguments?.getLong("messageId", -1L) ?: -1L
        if (messageId != -1L) {
            val args = Bundle()
            args.putLong("messageId", messageId)
            arguments?.remove("messageId")
            activity.navigate(navTarget = NavTarget.MESSAGE, args = args)
            return
        }

        val args = arguments

        val pagerAdapter = FragmentLazyPagerAdapter(
            fragmentManager = parentFragmentManager,
            swipeRefreshLayout = null,
            fragments = listOf(
                MessagesListFragment().apply {
                    onPageDestroy = this@MessagesFragment.onPageDestroy
                    arguments = Bundle("messageType" to Message.TYPE_RECEIVED)
                    args?.getBundle("page0")?.let {
                        arguments?.putAll(it)
                    }
                } to getString(R.string.messages_tab_received),

                MessagesListFragment().apply {
                    onPageDestroy = this@MessagesFragment.onPageDestroy
                    arguments = Bundle("messageType" to Message.TYPE_SENT)
                    args?.getBundle("page1")?.let {
                        arguments?.putAll(it)
                    }
                } to getString(R.string.messages_tab_sent),

                MessagesListFragment().apply {
                    onPageDestroy = this@MessagesFragment.onPageDestroy
                    arguments = Bundle("messageType" to Message.TYPE_DELETED)
                    args?.getBundle("page2")?.let {
                        arguments?.putAll(it)
                    }
                } to getString(R.string.messages_tab_deleted),

                MessagesListFragment().apply {
                    onPageDestroy = this@MessagesFragment.onPageDestroy
                    arguments = Bundle("messageType" to Message.TYPE_DRAFT)
                    args?.getBundle("page3")?.let {
                        arguments?.putAll(it)
                    }
                } to getString(R.string.messages_tab_draft),
            ),
        )
        b.viewPager.apply {
            offscreenPageLimit = 1
            adapter = pagerAdapter
            currentItem = pageSelection
            addOnPageSelectedListener {
                pageSelection = it
            }
            b.tabLayout.setupWithViewPager(this)
        }
    }

    override suspend fun onFabClick() {
        activity.navigate(navTarget = NavTarget.MESSAGE_COMPOSE)
    }

    private val onPageDestroy = { position: Int, outState: Bundle? ->
        arguments?.putBundle("page$position", outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        (b.viewPager.adapter as? FragmentLazyPagerAdapter)?.fragments?.forEach {
            it.first.onDestroy()
        }
    }
}
