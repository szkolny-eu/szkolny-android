/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial.Icon
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.databinding.FragmentHomeBinding
import pl.szczodrzynski.edziennik.ui.dialogs.home.StudentNumberDialog
import pl.szczodrzynski.edziennik.ui.modules.home.cards.*
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import kotlin.coroutines.CoroutineContext

class HomeFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "HomeFragment"

        fun swapCards(fromPosition: Int, toPosition: Int, cardAdapter: HomeCardAdapter): Boolean {
            val fromCard = cardAdapter.items[fromPosition]
            val toCard = cardAdapter.items[toPosition]
            if (fromCard.id >= 100 || toCard.id >= 100) {
                // debug & archive cards are not swappable
                return false
            }
            cardAdapter.items[fromPosition] = cardAdapter.items[toPosition]
            cardAdapter.items[toPosition] = fromCard
            cardAdapter.notifyItemMoved(fromPosition, toPosition)

            val homeCards = App.config.forProfile().ui.homeCards.toMutableList()
            val fromIndex = homeCards.indexOfFirst { it.cardId == fromCard.id }
            val toIndex = homeCards.indexOfFirst { it.cardId == toCard.id }
            val fromPair = homeCards[fromIndex]
            homeCards[fromIndex] = homeCards[toIndex]
            homeCards[toIndex] = fromPair
            App.config.forProfile().ui.homeCards = homeCards
            return true
        }

        fun removeCard(position: Int, cardAdapter: HomeCardAdapter) {
            val homeCards = App.config.forProfile().ui.homeCards.toMutableList()
            if (position >= homeCards.size)
                return
            val card = cardAdapter.items[position]
            if (card.id >= 100) {
                // debug & archive cards are not removable
                //cardAdapter.notifyDataSetChanged()
                return
            }
            homeCards.removeAll { it.cardId == card.id }
            App.config.forProfile().ui.homeCards = homeCards
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
        if (!isAdded)
            return

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_add_remove_cards)
                        .withIcon(Icon.cmd_card_bulleted_settings_outline)
                        .withOnClickListener(OnClickListener {
                            activity.bottomSheet.close()
                            HomeConfigDialog(activity, reloadOnDismiss = true)
                        }),
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
                            launch { withContext(Dispatchers.Default) {
                                if (app.profile.loginStoreType != LoginStore.LOGIN_TYPE_LIBRUS) {
                                    app.db.metadataDao().setAllSeenExceptMessagesAndAnnouncements(App.profileId, true)
                                } else {
                                    app.db.metadataDao().setAllSeenExceptMessages(App.profileId, true)
                                }
                            } }

                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show()
                        })
        )
        b.configureCards.onClick {
            HomeConfigDialog(activity, reloadOnDismiss = true)
        }

        b.scrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int ->
            b.refreshLayout.isEnabled = scrollY == 0
        }

        val showUnified = false

        val cards = app.config.forProfile().ui.homeCards.filter { it.profileId == app.profile.id }.toMutableList()
        if (cards.isEmpty()) {
            cards += listOf(
                    HomeCardModel(app.profile.id, HomeCard.CARD_LUCKY_NUMBER),
                    HomeCardModel(app.profile.id, HomeCard.CARD_TIMETABLE),
                    HomeCardModel(app.profile.id, HomeCard.CARD_EVENTS),
                    HomeCardModel(app.profile.id, HomeCard.CARD_GRADES)
            )
            app.config.forProfile().ui.homeCards = app.config.forProfile().ui.homeCards.toMutableList().also { it.addAll(cards) }
        }

        val items = mutableListOf<HomeCard>()
        cards.mapNotNullTo(items) {
            @Suppress("USELESS_CAST")
            when (it.cardId) {
                HomeCard.CARD_LUCKY_NUMBER -> HomeLuckyNumberCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_TIMETABLE -> HomeTimetableCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_GRADES -> HomeGradesCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_EVENTS -> HomeEventsCard(it.cardId, app, activity, this, app.profile)
                else -> null
            } as HomeCard?
        }
        //if (App.devMode)
        //    items += HomeDebugCard(100, app, activity, this, app.profile)
        if (app.profile.archived)
            items.add(0, HomeArchiveCard(101, app, activity, this, app.profile))

        val status = app.config.sync.registerAvailability[app.profile.registerName]
        val update = app.config.update
        if (update != null && update.versionCode > BuildConfig.VERSION_CODE
                || status != null && (!status.available || status.minVersionCode > BuildConfig.VERSION_CODE)) {
            items.add(0, HomeAvailabilityCard(102, app, activity, this, app.profile))
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
