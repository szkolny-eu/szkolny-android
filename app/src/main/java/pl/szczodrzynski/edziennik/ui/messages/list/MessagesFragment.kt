package pl.szczodrzynski.edziennik.ui.messages.list

import android.os.Bundle
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.databinding.BasePagerFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ui.base.fragment.PagerFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.MessagesConfigDialog
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem

class MessagesFragment : PagerFragment<BasePagerFragmentBinding, MainActivity>(
    inflater = BasePagerFragmentBinding::inflate,
) {
    companion object {
        var pageSelection = 0
    }

    override fun getFab() = R.string.compose to CommunityMaterial.Icon3.cmd_pencil_outline
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_message_compose)
            .withIcon(CommunityMaterial.Icon3.cmd_pencil_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                activity.navigate(navTarget = NavTarget.MESSAGE_COMPOSE)
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_messages_config)
            .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                MessagesConfigDialog(activity, false, null, null).show()
            }
    )

    override fun getTabLayout() = b.tabLayout
    override fun getViewPager() = b.viewPager
    override suspend fun onCreatePages() = listOf(
        MessagesListFragment().apply {
            arguments = Bundle("messageType" to Message.TYPE_RECEIVED)
        } to getString(R.string.messages_tab_received),
        MessagesListFragment().apply {
            arguments = Bundle("messageType" to Message.TYPE_SENT)
        } to getString(R.string.messages_tab_sent),
        MessagesListFragment().apply {
            arguments = Bundle("messageType" to Message.TYPE_DELETED)
        } to getString(R.string.messages_tab_deleted),
        MessagesListFragment().apply {
            arguments = Bundle("messageType" to Message.TYPE_DRAFT)
        } to getString(R.string.messages_tab_draft),
    )

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        val messageId = arguments?.getLong("messageId", -1L) ?: -1L
        if (messageId != -1L) {
            val args = Bundle()
            args.putLong("messageId", messageId)
            arguments?.remove("messageId")
            activity.navigate(navTarget = NavTarget.MESSAGE, args = args)
        }

        super.onViewReady(savedInstanceState)
    }

    override suspend fun onFabClick() {
        activity.navigate(navTarget = NavTarget.MESSAGE_COMPOSE)
    }
}
