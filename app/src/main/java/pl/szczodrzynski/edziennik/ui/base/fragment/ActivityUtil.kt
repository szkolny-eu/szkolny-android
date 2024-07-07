/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-3.
 */

package pl.szczodrzynski.edziennik.ui.base.fragment

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.login.LoginActivity
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem

@SuppressLint("ClickableViewAccessibility")
internal fun BaseFragment<*, *>.setupCanRefresh() {
    when (val view = getRefreshScrollingView()) {
        is RecyclerView -> {
            canRefresh = !view.canScrollVertically(-1)
            view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    // disable refresh when scrolled down
                    if (recyclerView.canScrollVertically(-1))
                        canRefresh = false
                    // enable refresh when scrolled to the top and not scrolling anymore
                    else if (newState == RecyclerView.SCROLL_STATE_IDLE)
                        canRefresh = true
                }
            })
        }

        is View -> {
            canRefresh = !view.canScrollVertically(-1)
            var isTouched = false
            view.setOnTouchListener { _, event ->
                // keep track of the touch state
                when (event.action) {
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isTouched = false
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> isTouched = true
                }
                // disable refresh when scrolled down
                if (view.canScrollVertically(-1))
                    canRefresh = false
                // enable refresh when scrolled to the top and not touching anymore
                else if (!isTouched)
                    canRefresh = true
                false
            }
        }

        else -> {
            // dispatch the default value to the activity
            canRefresh = canRefresh
        }
    }
}

internal fun BaseFragment<*, *>.setupMainActivity(activity: MainActivity) {
    val items = getBottomSheetItems().toMutableList()
    getMarkAsReadType()?.let { metadataType ->
        if (items.isNotEmpty())
            items += BottomSheetSeparatorItem(true)
        items += BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_mark_as_read)
            .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                launch(Dispatchers.IO) {
                    app.db.metadataDao().setAllSeen(app.profileId, metadataType, true)
                }
                Toast.makeText(
                    activity,
                    R.string.main_menu_mark_as_read_success,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    if (items.isNotEmpty()) {
        activity.navView.bottomSheet.prependItems(*items.toTypedArray())
    }

    getFab()?.let { (text, icon) ->
        activity.navView.bottomBar.apply {
            fabEnable = true
            fabExtendedText = app.getString(text)
            fabIcon = icon
            setFabOnClickListener {
                launch {
                    onFabClick()
                }
            }
        }
    }
}

internal fun BaseFragment<*, *>.setupLoginActivity(activity: LoginActivity) {}
