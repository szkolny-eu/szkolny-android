package pl.szczodrzynski.edziennik.ui.modules.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.databinding.MessagesFragmentBinding
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.FragmentLazyPagerAdapter
import kotlin.coroutines.CoroutineContext

class MessagesFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "MessagesFragment"
        var pageSelection = 0
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: MessagesFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = MessagesFragmentBinding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        val messageId = arguments?.getLong("messageId", -1L) ?: -1L
        if (messageId != -1L) {
            val args = Bundle()
            args.putLong("messageId", messageId)
            arguments?.remove("messageId")
            activity.loadTarget(MainActivity.TARGET_MESSAGES_DETAILS, args)
            return
        }

        val args = arguments

        val pagerAdapter = FragmentLazyPagerAdapter(
                fragmentManager ?: return,
                b.refreshLayout,
                listOf(
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
                        } to getString(R.string.messages_tab_deleted)
                )
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

        activity.navView.apply {
            bottomBar.apply {
                fabEnable = true
                fabExtendedText = getString(R.string.compose)
                fabIcon = CommunityMaterial.Icon2.cmd_pencil_outline
            }

            setFabOnClickListener(View.OnClickListener {
                activity.loadTarget(MainActivity.TARGET_MESSAGES_COMPOSE)
            })
        }

        activity.gainAttentionFAB()
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
