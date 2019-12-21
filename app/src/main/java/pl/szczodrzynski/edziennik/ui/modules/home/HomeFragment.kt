/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial.Icon
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentHomeBinding
import pl.szczodrzynski.edziennik.ui.dialogs.home.StudentNumberDialog
import pl.szczodrzynski.edziennik.ui.modules.home.cards.HomeGradesCard
import pl.szczodrzynski.edziennik.ui.modules.home.cards.HomeLuckyNumberCard
import pl.szczodrzynski.edziennik.ui.modules.home.cards.HomeTimetableCard
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import kotlin.coroutines.CoroutineContext

class HomeFragmentV2 : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "HomeFragmentOld"

        fun swapCards(fromPosition: Int, toPosition: Int, cardAdapter: HomeCardAdapter) {
            val homeCards = App.getConfig().ui.homeCards.toMutableList()
            val fromPair = homeCards[fromPosition]
            homeCards[fromPosition] = homeCards[toPosition]
            homeCards[toPosition] = fromPair
            App.getConfig().ui.homeCards = homeCards

            val fromCard = cardAdapter.items[fromPosition]
            cardAdapter.items[fromPosition] = cardAdapter.items[toPosition]
            cardAdapter.items[toPosition] = fromCard
            cardAdapter.notifyItemMoved(fromPosition, toPosition)
        }
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentHomeBinding

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
        b = FragmentHomeBinding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        job = Job()
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_set_student_number)
                        .withIcon(SzkolnyFont.Icon.szf_clipboard_list_outline)
                        .withOnClickListener(OnClickListener {
                            activity.bottomSheet.close()
                            StudentNumberDialog(activity, app.profile) {
                                app.profileSave()
                            }
                        }),
                BottomSheetSeparatorItem(true),
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_everything_as_read)
                        .withIcon(Icon.cmd_eye_check_outline)
                        .withOnClickListener(OnClickListener {
                            activity.bottomSheet.close()
                            AsyncTask.execute { app.db.metadataDao().setAllSeen(App.profileId, true) }
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show()
                        })
        )

        val showUnified = false

        val cards = app.config.ui.homeCards.filter { it.profileId == app.profile.id }.toMutableList()
        if (cards.isEmpty()) {
            cards += listOf(
                    HomeCardModel(app.profile.id, HomeCard.CARD_LUCKY_NUMBER),
                    HomeCardModel(app.profile.id, HomeCard.CARD_TIMETABLE),
                    /*HomeCardModel(app.profile.id, HomeCard.CARD_EVENTS),*/
                    HomeCardModel(app.profile.id, HomeCard.CARD_GRADES)
            )
            app.config.ui.homeCards = app.config.ui.homeCards.toMutableList().also { it.addAll(cards) }
        }

        val items = mutableListOf<HomeCard>()
        cards.mapNotNullTo(items) {
            when (it.cardId) {
                HomeCard.CARD_LUCKY_NUMBER -> HomeLuckyNumberCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_TIMETABLE -> HomeTimetableCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_GRADES -> HomeGradesCard(it.cardId, app, activity, this, app.profile)
                else -> null
            }
        }

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
