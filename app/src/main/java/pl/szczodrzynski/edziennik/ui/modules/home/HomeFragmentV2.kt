/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentHomeV2Binding
import pl.szczodrzynski.edziennik.ui.modules.home.cards.HomeLuckyNumberCard
import pl.szczodrzynski.edziennik.utils.Themes
import kotlin.coroutines.CoroutineContext

class HomeFragmentV2 : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "HomeFragment"

        fun swapCards(fromPosition: Int, toPosition: Int, cardAdapter: HomeCardAdapter) {
            val fromCard = cardAdapter.items[fromPosition]
            cardAdapter.items[fromPosition] = cardAdapter.items[toPosition]
            cardAdapter.items[toPosition] = fromCard
            cardAdapter.notifyItemMoved(fromPosition, toPosition)
        }
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentHomeV2Binding

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
        b = FragmentHomeV2Binding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        job = Job()
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        val items = mutableListOf<HomeCard>(
                HomeLuckyNumberCard(0, app, activity, this, app.profile),
                HomeDummyCard(1),
                HomeDummyCard(2),
                HomeDummyCard(3),
                HomeDummyCard(4),
                HomeDummyCard(5),
                HomeDummyCard(6),
                HomeDummyCard(7),
                HomeDummyCard(8),
                HomeDummyCard(9),
                HomeDummyCard(10),
                HomeDummyCard(11),
                HomeDummyCard(12),
                HomeDummyCard(13),
                HomeDummyCard(14),
                HomeDummyCard(15),
                HomeDummyCard(16)
        )

        val adapter = HomeCardAdapter(items)
        val itemTouchHelper = ItemTouchHelper(CardItemTouchHelperCallback(adapter, b.refreshLayout))
        adapter.itemTouchHelper = itemTouchHelper
        b.list.layoutManager = LinearLayoutManager(activity)
        b.list.adapter = adapter
        b.list.setAccessibilityDelegateCompat(object : RecyclerViewAccessibilityDelegate(b.list) {
            override fun getItemDelegate(): AccessibilityDelegateCompat {
                return object : ItemDelegate(this) {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        val position: Int = b.list.getChildLayoutPosition(host)
                        if (position != 0) {
                            info.addAction(AccessibilityActionCompat(
                                    R.id.move_card_up_action,
                                    host.resources.getString(R.string.card_action_move_up)
                            ))
                        }
                        if (position != adapter.itemCount - 1) {
                            info.addAction(AccessibilityActionCompat(
                                    R.id.move_card_down_action,
                                    host.resources.getString(R.string.card_action_move_down)
                            ))
                        }
                    }

                    override fun performAccessibilityAction(host: View, action: Int, args: Bundle): Boolean {
                        val fromPosition: Int = b.list.getChildLayoutPosition(host)
                        if (action == R.id.move_card_down_action) {
                            swapCards(fromPosition, fromPosition + 1, adapter)
                            return true
                        } else if (action == R.id.move_card_up_action) {
                            swapCards(fromPosition, fromPosition - 1, adapter)
                            return true
                        }
                        return super.performAccessibilityAction(host, action, args)
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(b.list)
    }
}