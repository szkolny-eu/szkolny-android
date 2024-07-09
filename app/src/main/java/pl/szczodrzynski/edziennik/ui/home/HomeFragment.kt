/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial.Icon
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.databinding.FragmentHomeBinding
import pl.szczodrzynski.edziennik.ext.hasUIFeature
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.StudentNumberDialog
import pl.szczodrzynski.edziennik.ui.home.cards.HomeArchiveCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeAvailabilityCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeEventsCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeGradesCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeLuckyNumberCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeNotesCard
import pl.szczodrzynski.edziennik.ui.home.cards.HomeTimetableCard
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem

class HomeFragment : BaseFragment<FragmentHomeBinding, MainActivity>(
    inflater = FragmentHomeBinding::inflate,
) {
    companion object {
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

            val homeCards = App.profile.config.ui.homeCards.toMutableList()
            val fromIndex = homeCards.indexOfFirst { it.cardId == fromCard.id }
            val toIndex = homeCards.indexOfFirst { it.cardId == toCard.id }
            val fromPair = homeCards[fromIndex]
            homeCards[fromIndex] = homeCards[toIndex]
            homeCards[toIndex] = fromPair
            App.profile.config.ui.homeCards = homeCards
            return true
        }

        fun removeCard(position: Int, cardAdapter: HomeCardAdapter) {
            val homeCards = App.profile.config.ui.homeCards.toMutableList()
            if (position >= homeCards.size)
                return
            val card = cardAdapter.items[position]
            if (card.id >= 100) {
                // debug & archive cards are not removable
                //cardAdapter.notifyDataSetChanged()
                return
            }
            homeCards.removeAll { it.cardId == card.id }
            App.profile.config.ui.homeCards = homeCards
        }
    }

    override fun getScrollingView() = b.scrollView
    override fun getSyncParams() = null to null
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_add_remove_cards)
            .withIcon(Icon.cmd_card_bulleted_settings_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                HomeConfigDialog(activity, reloadOnDismiss = true).show()
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_set_student_number)
            .withIcon(SzkolnyFont.Icon.szf_clipboard_list_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                StudentNumberDialog(activity, app.profile).show()
            },
        BottomSheetSeparatorItem(true),
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_mark_everything_as_read)
            .withIcon(Icon.cmd_eye_check_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                launch(Dispatchers.IO) {
                    if (!app.data.uiConfig.enableMarkAsReadAnnouncements) {
                        app.db.metadataDao()
                            .setAllSeenExceptMessagesAndAnnouncements(App.profileId, true)
                    } else {
                        app.db.metadataDao().setAllSeenExceptMessages(App.profileId, true)
                    }
                }

                Toast.makeText(
                    activity,
                    R.string.main_menu_mark_as_read_success,
                    Toast.LENGTH_SHORT
                ).show()
            }
    )

    private val manager
        get() = app.permissionManager

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        if (!manager.isNotificationPermissionGranted) {
            manager.requestNotificationsPermission(activity, 0, false){}
        }

        b.configureCards.onClick {
            HomeConfigDialog(activity, reloadOnDismiss = true).show()
        }

        val cards = app.profile.config.ui.homeCards.filter { it.profileId == app.profile.id }.toMutableList()
        if (cards.isEmpty()) {
            cards += listOfNotNull(
                    HomeCardModel(app.profile.id, HomeCard.CARD_LUCKY_NUMBER).takeIf { app.profile.hasUIFeature(
                        FeatureType.LUCKY_NUMBER) },
                    HomeCardModel(app.profile.id, HomeCard.CARD_TIMETABLE).takeIf { app.profile.hasUIFeature(
                        FeatureType.TIMETABLE) },
                    HomeCardModel(app.profile.id, HomeCard.CARD_EVENTS).takeIf { app.profile.hasUIFeature(
                        FeatureType.AGENDA) },
                    HomeCardModel(app.profile.id, HomeCard.CARD_GRADES).takeIf { app.profile.hasUIFeature(
                        FeatureType.GRADES) },
                    HomeCardModel(app.profile.id, HomeCard.CARD_NOTES),
            )
            app.profile.config.ui.homeCards = app.profile.config.ui.homeCards.toMutableList().also { it.addAll(cards) }
        }

        val items = mutableListOf<HomeCard>()
        cards.mapNotNullTo(items) {
            @Suppress("USELESS_CAST")
            when (it.cardId) {
                HomeCard.CARD_LUCKY_NUMBER -> HomeLuckyNumberCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_TIMETABLE -> HomeTimetableCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_GRADES -> HomeGradesCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_EVENTS -> HomeEventsCard(it.cardId, app, activity, this, app.profile)
                HomeCard.CARD_NOTES -> HomeNotesCard(it.cardId, app, activity, this, app.profile)
                else -> null
            } as HomeCard?
        }
        //if (App.devMode)
        //    items += HomeDebugCard(100, app, activity, this, app.profile)
        if (app.profile.archived)
            items.add(0, HomeArchiveCard(101, app, activity, this, app.profile))

        val status = app.availabilityManager.check(app.profile, cacheOnly = true)?.status
        val update = app.config.update
        if (update != null && app.updateManager.isApplicable(update) || status?.userMessage != null) {
            items.add(0, HomeAvailabilityCard(102, app, activity, this, app.profile))
        }

        val adapter = HomeCardAdapter(items)
        val itemTouchHelper = ItemTouchHelper(CardItemTouchHelperCallback(adapter) {
            canRefreshDisabled = !it
        })
        adapter.itemTouchHelper = itemTouchHelper
        b.list.layoutManager = LinearLayoutManager(activity)
        b.list.adapter = adapter
        b.list.isNestedScrollingEnabled = false
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

                    override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
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
