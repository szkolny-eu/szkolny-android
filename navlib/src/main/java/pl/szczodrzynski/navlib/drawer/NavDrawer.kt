package pl.szczodrzynski.navlib.drawer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.customview.widget.ViewDragHelper
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ColorHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.BaseDrawerItem
import com.mikepenz.materialdrawer.model.MiniProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.*
import com.mikepenz.materialdrawer.util.*
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import com.mikepenz.materialdrawer.widget.MiniDrawerSliderView
import com.mikepenz.materialize.util.UIUtils
import pl.szczodrzynski.navlib.*
import pl.szczodrzynski.navlib.drawer.items.DrawerPrimaryItem

class NavDrawer(
    val context: Context,
    val drawerLayout: DrawerLayout,
    val drawerContainerLandscape: FrameLayout,
    val drawerContainerPortrait: FrameLayout,
    val miniDrawerElevation: View
) {
    companion object {
        private const val DRAWER_MODE_NORMAL = 0
        private const val DRAWER_MODE_MINI = 1
        private const val DRAWER_MODE_FIXED = 2
    }

    private lateinit var activity: Activity
    private val resources: Resources
        get() = context.resources

    internal lateinit var toolbar: NavToolbar
    internal lateinit var bottomBar: NavBottomBar

    private lateinit var drawer: MaterialDrawerSliderView
    private lateinit var accountHeader: AccountHeaderView
    private lateinit var miniDrawer: MiniDrawerSliderView

    private var drawerMode: Int = DRAWER_MODE_NORMAL
    private var selection: Int = -1

    lateinit var badgeStyle: BadgeStyle

    @SuppressLint("ClickableViewAccessibility")
    fun init(activity: Activity) {
        this.activity = activity

        /*badgeStyle = BadgeStyle(
            R.drawable.material_drawer_badge,
            getColorFromAttr(context, R.attr.colorError),
            getColorFromAttr(context, R.attr.colorError),
            getColorFromAttr(context, R.attr.colorOnError)
        )*/

        badgeStyle = BadgeStyle().apply {
            textColor = ColorHolder.fromColor(Color.WHITE)
            color = ColorHolder.fromColor(0xffd32f2f.toInt())
        }

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerClosed(drawerView: View) {
                drawerClosedListener?.invoke()
                profileSelectionClose()
            }
            override fun onDrawerOpened(drawerView: View) {
                drawerOpenedListener?.invoke()
            }
        })

        accountHeader = AccountHeaderView(context).apply {
            dividerBelowHeader = false
            headerBackground = ImageHolder(R.drawable.header)
            displayBadgesOnSmallProfileImages = true

            onAccountHeaderListener = { view, profile, current ->
                if (profile is ProfileSettingDrawerItem) {
                    drawerProfileSettingClickListener?.invoke(profile.identifier.toInt(), view) ?: false
                }
                else {
                    updateBadges()
                    if (current) {
                        close()
                        profileSelectionClose()
                        true
                    }
                    else {
                        (drawerProfileSelectedListener?.invoke(profile.identifier.toInt(), profile, current, view) ?: false).also {
                            setToolbarProfileImage(profileList.singleOrNull { it.id == profile.identifier.toInt() })
                        }
                    }
                }
            }

            onAccountHeaderItemLongClickListener = { view, profile, current ->
                if (profile is ProfileSettingDrawerItem) {
                    drawerProfileSettingLongClickListener?.invoke(profile.identifier.toInt(), view) ?: true
                }
                else {
                    drawerProfileLongClickListener?.invoke(profile.identifier.toInt(), profile, current, view) ?: false
                }
            }

            onAccountHeaderProfileImageListener = { view, profile, current ->
                drawerProfileImageClickListener?.invoke(profile.identifier.toInt(), profile, current, view) ?: false
            }
            //.withTextColor(ContextCompat.getColor(context, R.color.material_drawer_dark_primary_text))
        }

        drawer = MaterialDrawerSliderView(context).apply {
            accountHeader = this@NavDrawer.accountHeader
            itemAnimator = AlphaCrossFadeAnimator()
            //hasStableIds = true

            onDrawerItemClickListener = { _, drawerItem, position ->
                if (drawerItem.identifier.toInt() == selection) {
                    false
                }
                else {
                    val consumed = drawerItemSelectedListener?.invoke(drawerItem.identifier.toInt(), position, drawerItem)
                    if (consumed == false || !drawerItem.isSelectable) {
                        setSelection(selection, false)
                        consumed == false
                    }
                    else if (consumed == true) {
                        when (drawerItem) {
                            is DrawerPrimaryItem -> toolbar.title = drawerItem.appTitle ?: drawerItem.name?.getText(context) ?: ""
                            is BaseDrawerItem<*, *> -> toolbar.title = drawerItem.name?.getText(context) ?: ""
                        }
                        false
                    }
                    else {
                        false
                    }
                }
            }

            onDrawerItemLongClickListener = { _, drawerItem, position ->
                drawerItemLongClickListener?.invoke(drawerItem.identifier.toInt(), position, drawerItem) ?: true
            }
        }

        setOnApplyWindowInsetsListener(drawer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. Here the system is setting
            // only the bottom, left, and right dimensions, but apply whichever insets are
            // appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)

            // Return CONSUMED if you don't want want the window insets to keep being
            // passed down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        miniDrawer = MiniDrawerSliderView(context).apply {
            drawer = this@NavDrawer.drawer
            includeSecondaryDrawerItems = false
            try {
                this::class.java.getDeclaredField("onMiniDrawerItemClickListener").let {
                    it.isAccessible = true
                    it.set(this, { v: View?, position: Int, item: IDrawerItem<*>, type: Int ->
                        if (item is MiniProfileDrawerItem) {
                            profileSelectionOpen()
                            open()
                            true
                        } else false
                    })
                }
            } catch (_: Exception) { }
        }

        updateMiniDrawer()

        toolbar.profileImageClickListener = {
            profileSelectionOpen()
            open()
        }

        val configuration = context.resources.configuration
        decideDrawerMode(
            configuration.orientation,
            configuration.screenWidthDp,
            configuration.screenHeightDp
        )
    }

    fun setItems(vararg items: IDrawerItem<*>) {
        drawer.removeAllItems()
        drawer.addItems(*items)
        updateMiniDrawer()
    }

    /*    _____      _            _                        _   _               _
         |  __ \    (_)          | |                      | | | |             | |
         | |__) | __ ___   ____ _| |_ ___   _ __ ___   ___| |_| |__   ___   __| |___
         |  ___/ '__| \ \ / / _` | __/ _ \ | '_ ` _ \ / _ \ __| '_ \ / _ \ / _` / __|
         | |   | |  | |\ V / (_| | ||  __/ | | | | | |  __/ |_| | | | (_) | (_| \__ \
         |_|   |_|  |_| \_/ \__,_|\__\___| |_| |_| |_|\___|\__|_| |_|\___/ \__,_|__*/
    private fun drawerSetDragMargin(size: Float) {
        try {
            val mDrawerLayout = drawerLayout
            val mDragger = mDrawerLayout::class.java.getDeclaredField(
                "mLeftDragger"
            )
            mDragger.isAccessible = true
            val draggerObj = mDragger.get(mDrawerLayout) as ViewDragHelper?
            draggerObj?.edgeSize = size.toInt()

            // update for SDK >= 29 (Android 10)
            val useSystemInsets = mDrawerLayout::class.java.getDeclaredField(
                "sEdgeSizeUsingSystemGestureInsets"
            )
            useSystemInsets.isAccessible = true
            useSystemInsets.set(null, false)
        }
        catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Oops, proguard works", Toast.LENGTH_SHORT).show()
        }
    }

    var miniDrawerVisiblePortrait: Boolean? = null
        set(value) {
            field = value
            val configuration = context.resources.configuration
            decideDrawerMode(
                configuration.orientation,
                configuration.screenWidthDp,
                configuration.screenHeightDp
            )
        }
    var miniDrawerVisibleLandscape: Boolean? = null
        set(value) {
            field = value
            val configuration = context.resources.configuration
            decideDrawerMode(
                configuration.orientation,
                configuration.screenWidthDp,
                configuration.screenHeightDp
            )
        }

    internal fun decideDrawerMode(orientation: Int, widthDp: Int, heightDp: Int) {
        val drawerLayoutParams = DrawerLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
            gravity = Gravity.START
        }
        val fixedLayoutParams = FrameLayout.LayoutParams(UIUtils.convertDpToPixel(300f, context).toInt(), MATCH_PARENT)

        Log.d("NavLib", "Deciding drawer mode:")
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d("NavLib", "- fixed container disabled")

            if (drawerContainerLandscape.childCount > 0) {
                drawerContainerLandscape.removeAllViews()
            }
            Log.d("NavLib", "- mini drawer land disabled")

            if (drawerLayout.indexOfChild(drawer) == -1) {
                drawerLayout.addView(drawer, drawerLayoutParams)
            }
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            Log.d("NavLib", "- slider enabled")

            if ((widthDp >= 480 && miniDrawerVisiblePortrait != false) || miniDrawerVisiblePortrait == true) {
                if (drawerContainerPortrait.indexOfChild(miniDrawer) == -1)
                    drawerContainerPortrait.addView(miniDrawer)
                Log.d("NavLib", "- mini drawer port enabled")
                drawerSetDragMargin(72 * resources.displayMetrics.density)
                drawerMode = DRAWER_MODE_MINI
                updateMiniDrawer()
            }
            else {
                if (drawerContainerPortrait.childCount > 0) {
                    drawerContainerPortrait.removeAllViews()
                }
                Log.d("NavLib", "- mini drawer port disabled")
                drawerSetDragMargin(20 * resources.displayMetrics.density)
                drawerMode = DRAWER_MODE_NORMAL
            }
        }
        else {
            if (drawerContainerPortrait.childCount > 0) {
                drawerContainerPortrait.removeAllViews()
            }
            Log.d("NavLib", "- mini drawer port disabled")

            if ((widthDp in 480 until 900 && miniDrawerVisibleLandscape != false) || miniDrawerVisibleLandscape == true) {
                if (drawerContainerLandscape.indexOfChild(miniDrawer) == -1)
                    drawerContainerLandscape.addView(miniDrawer)
                Log.d("NavLib", "- mini drawer land enabled")
                drawerSetDragMargin(72 * resources.displayMetrics.density)
                drawerMode = DRAWER_MODE_MINI
                updateMiniDrawer()
            }
            else {
                if (drawerContainerLandscape.childCount > 0) {
                    drawerContainerLandscape.removeAllViews()
                }
                Log.d("NavLib", "- mini drawer land disabled")
                drawerSetDragMargin(20 * resources.displayMetrics.density)
                drawerMode = DRAWER_MODE_NORMAL
            }
            if (widthDp >= 900) {
                // screen is big enough to show fixed drawer
                if (drawerLayout.indexOfChild(drawer) != -1) {
                    // remove from slider
                    drawerLayout.removeView(drawer)
                }
                // lock the slider
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                Log.d("NavLib", "- slider disabled")
                // add to fixed container
                if (drawerContainerLandscape.indexOfChild(drawer) == -1)
                    drawerContainerLandscape.addView(drawer, fixedLayoutParams)
                drawer.visibility = View.VISIBLE
                Log.d("NavLib", "- fixed container enabled")
                drawerMode = DRAWER_MODE_FIXED
            }
            else {
                // screen is too small for the fixed drawer
                if (drawerContainerLandscape.indexOfChild(drawer) != -1) {
                    // remove from fixed container
                    drawerContainerLandscape.removeView(drawer)
                }
                Log.d("NavLib", "- fixed container disabled")
                // unlock the slider
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                if (drawerLayout.indexOfChild(drawer) == -1) {
                    // add to slider
                    drawerLayout.addView(drawer, drawerLayoutParams)
                }
                Log.d("NavLib", "- slider enabled")
            }
        }

        miniDrawerElevation.visibility = if (drawerMode == DRAWER_MODE_MINI || drawerMode == DRAWER_MODE_FIXED) View.VISIBLE else View.GONE
    }

    private fun updateMiniDrawer() {
        selection = drawer.selectedItemIdentifier.toInt()
        //if (drawerMode == DRAWER_MODE_MINI)
        miniDrawer.createItems()
    }

    /*    _____       _     _ _                       _   _               _
         |  __ \     | |   | (_)                     | | | |             | |
         | |__) |   _| |__ | |_  ___   _ __ ___   ___| |_| |__   ___   __| |___
         |  ___/ | | | '_ \| | |/ __| | '_ ` _ \ / _ \ __| '_ \ / _ \ / _` / __|
         | |   | |_| | |_) | | | (__  | | | | | |  __/ |_| | | | (_) | (_| \__ \
         |_|    \__,_|_.__/|_|_|\___| |_| |_| |_|\___|\__|_| |_|\___/ \__,_|__*/
    var isOpen
        get() = drawerLayout.isOpen || drawerMode == DRAWER_MODE_FIXED
        set(value) {
            if (drawerMode == DRAWER_MODE_FIXED)
                return
            if (value && !isOpen) drawerLayout.open() else if (!value && isOpen) drawerLayout.close()
        }
    fun open() { isOpen = true }
    fun close() { isOpen = false }
    fun toggle() { isOpen = !isOpen }

    var profileSelectionIsOpen
        get() = accountHeader.selectionListShown
        set(value) {
            if (value != profileSelectionIsOpen)
                profileSelectionToggle()
        }
    fun profileSelectionOpen() { profileSelectionIsOpen = true }
    fun profileSelectionClose() { profileSelectionIsOpen = false }
    fun profileSelectionToggle() { accountHeader.selectionListShown = !accountHeader.selectionListShown }

    var drawerOpenedListener: (() -> Unit)? = null
    var drawerClosedListener: (() -> Unit)? = null
    var drawerItemSelectedListener: ((id: Int, position: Int, drawerItem: IDrawerItem<*>) -> Boolean)? = null
    var drawerItemLongClickListener: ((id: Int, position: Int, drawerItem: IDrawerItem<*>) -> Boolean)? = null
    var drawerProfileSelectedListener: ((id: Int, profile: IProfile, current: Boolean, view: View?) -> Boolean)? = null
    var drawerProfileLongClickListener: ((id: Int, profile: IProfile, current: Boolean, view: View?) -> Boolean)? = null
    var drawerProfileImageClickListener: ((id: Int, profile: IProfile, current: Boolean, view: View) -> Boolean)? = null
    var drawerProfileImageLongClickListener: ((id: Int, profile: IProfile, current: Boolean, view: View) -> Boolean)? = null
    var drawerProfileListEmptyListener: (() -> Unit)? = null
    var drawerProfileSettingClickListener: ((id: Int, view: View?) -> Boolean)? = null
    var drawerProfileSettingLongClickListener: ((id: Int, view: View?) -> Boolean)? = null

    fun miniDrawerEnabled(): Boolean = drawerMode == DRAWER_MODE_MINI
    fun fixedDrawerEnabled(): Boolean = drawerMode == DRAWER_MODE_FIXED

    fun setSelection(id: Int, fireOnClick: Boolean = true) {
        Log.d("NavDebug", "setSelection(id = $id, fireOnClick = $fireOnClick)")
        // seems that this cannot be open, because the itemAdapter has Profile items
        // instead of normal Drawer items...
        profileSelectionClose()
        selection = id

        if (drawer.selectedItemIdentifier != id.toLong()) {

        }

        if (drawer.selectedItemIdentifier != id.toLong() || !fireOnClick)
            drawer.setSelectionAtPosition(drawer.getPosition(id.toLong()), fireOnClick)

        miniDrawer.setSelection(-1L)
        if (drawerMode == DRAWER_MODE_MINI)
            miniDrawer.setSelection(id.toLong())
    }
    fun getSelection(): Int = selection

    // TODO 2019-08-27 add methods for Drawable, @DrawableRes
    fun setAccountHeaderBackground(path: String?) {
        if (path == null) {
            accountHeader.headerBackground = ImageHolder(R.drawable.header)
            return
        }
        accountHeader.headerBackground = ImageHolder(path)
    }

    /*    _____            __ _ _
         |  __ \          / _(_) |
         | |__) | __ ___ | |_ _| | ___  ___
         |  ___/ '__/ _ \|  _| | |/ _ \/ __|
         | |   | | | (_) | | | | |  __/\__ \
         |_|   |_|  \___/|_| |_|_|\___||__*/
    private var profileList: MutableList<IDrawerProfile> = mutableListOf()

    fun addProfileSettings(vararg items: ProfileSettingDrawerItem) {
        accountHeader.profiles?.addAll(items)
    }

    private fun updateProfileList() {
        // remove all profile items
        val profiles = accountHeader.profiles?.filterNot { it is ProfileDrawerItem } as MutableList<IProfile>?

        if (profileList.isEmpty())
            drawerProfileListEmptyListener?.invoke()

        profileList.forEachIndexed { index, profile ->
            val image = profile.getImageHolder(context)
            ProfileDrawerItem()
                .withIdentifier(profile.id.toLong())
                .withName(profile.name)
                .withEmail(profile.subname)
                .also { it.icon = image }
                .withBadgeStyle(badgeStyle)
                .withNameShown(true)
                .also { profiles?.add(index, it) }
        }

        accountHeader.profiles = profiles

        updateBadges()
        updateMiniDrawer()
    }

    fun setProfileList(profiles: MutableList<out IDrawerProfile>) {
        profileList = profiles as MutableList<IDrawerProfile>
        updateProfileList()
    }
    private var currentProfileObj: IDrawerProfile? = null
    val profileListEmpty: Boolean
        get() = profileList.isEmpty()
    var currentProfile: Int
        get() = accountHeader.activeProfile?.identifier?.toInt() ?: -1
        set(value) {
            Log.d("NavDebug", "currentProfile = $value")
            accountHeader.setActiveProfile(value.toLong(), false)
            currentProfileObj = profileList.singleOrNull { it.id == value }
            setToolbarProfileImage(currentProfileObj)
            updateBadges()
        }
    fun appendProfile(profile: IDrawerProfile) {
        profileList.add(profile)
        updateProfileList()
    }
    fun appendProfiles(vararg profiles: IDrawerProfile) {
        profileList.addAll(profiles)
        updateProfileList()
    }
    fun prependProfile(profile: IDrawerProfile) {
        profileList.add(0, profile)
        updateProfileList()
    }
    fun prependProfiles(vararg profiles: IDrawerProfile) {
        profileList.addAll(0, profiles.asList())
        updateProfileList()
    }
    fun addProfileAt(index: Int, profile: IDrawerProfile) {
        profileList.add(index, profile)
        updateProfileList()
    }
    fun addProfilesAt(index: Int, vararg profiles: IDrawerProfile) {
        profileList.addAll(index, profiles.asList())
        updateProfileList()
    }
    fun removeProfileById(id: Int) {
        profileList = profileList.filterNot { it.id == id }.toMutableList()
        updateProfileList()
    }
    fun removeProfileAt(index: Int) {
        profileList.removeAt(index)
        updateProfileList()
    }
    fun removeAllProfile() {
        profileList.clear()
        updateProfileList()
    }
    fun removeAllProfileSettings() {
        accountHeader.profiles = accountHeader.profiles?.filterNot { it is ProfileSettingDrawerItem }?.toMutableList()
    }

    fun getProfileById(id: Int, run: (it: IDrawerProfile?) -> Unit) {
        profileList.singleOrNull { it.id == id }.also {
            run(it)
            updateProfileList()
        }
    }
    fun getProfileByIndex(index: Int, run: (it: IDrawerProfile?) -> Unit) {
        profileList.getOrNull(index).also {
            run(it)
            updateProfileList()
        }
    }

    private fun setToolbarProfileImage(profile: IDrawerProfile?) {
        toolbar.profileImage = profile?.getImageDrawable(context)
    }


    /*    ____            _
         |  _ \          | |
         | |_) | __ _  __| | __ _  ___  ___
         |  _ < / _` |/ _` |/ _` |/ _ \/ __|
         | |_) | (_| | (_| | (_| |  __/\__ \
         |____/ \__,_|\__,_|\__, |\___||___/
                             __/ |
                            |__*/
    private var unreadCounterList: MutableList<IUnreadCounter> = mutableListOf()
    private val unreadCounterTypeMap = mutableMapOf<Int, Int>()

    fun updateBadges() {

        currentProfileObj = profileList.singleOrNull { it.id == currentProfile }

        drawer.itemAdapter.itemList.items.forEachIndexed { index, item ->
            if (item is Badgeable) {
                item.badge = null
                drawer.updateItem(item)
            }
        }

        var profileCounters = listOf<IUnreadCounter>()

        accountHeader.profiles?.forEach { profile ->
            if (profile !is ProfileDrawerItem) return@forEach
            val counters = unreadCounterList.filter { it.profileId == profile.identifier.toInt() }
            val count = counters.sumBy { it.count }
            val badge = when {
                count == 0 -> null
                count >= 99 -> StringHolder("99+")
                else -> StringHolder(count.toString())
            }
            if (profile.badge != badge) {
                profile.badge = badge
                accountHeader.updateProfile(profile)
            }

            if (currentProfile == profile.identifier.toInt())
                profileCounters = counters
        }

        Log.d("NavDebug", "updateBadges()")
        profileCounters.map {
            it.drawerItemId = unreadCounterTypeMap[it.type]
        }
        var totalCount = 0
        profileCounters.forEach {
            if (it.drawerItemId == null)
                return@forEach
            if (it.profileId != currentProfile) {
                //Log.d("NavDebug", "- Remove badge for ${it.drawerItemId}")
                //drawer?.updateBadge(it.drawerItemId?.toLong() ?: 0, null)
                return@forEach
            }
            Log.d("NavDebug", "- Set badge ${it.count} for ${it.drawerItemId}")
            drawer.updateBadge(
                it.drawerItemId?.toLong() ?: 0,
                when {
                    it.count == 0 -> null
                    it.count >= 99 -> StringHolder("99+")
                    else -> StringHolder(it.count.toString())
                }
            )
            totalCount += it.count
        }
        updateMiniDrawer()

        toolbar.navigationIcon?.setBadgeCount(totalCount)
        bottomBar.navigationIcon?.setBadgeCount(totalCount)
    }

    fun setUnreadCounterList(unreadCounterList: MutableList<out IUnreadCounter>) {
        this.unreadCounterList = unreadCounterList as MutableList<IUnreadCounter>
        updateBadges()
    }

    fun addUnreadCounterType(type: Int, drawerItem: Int) {
        unreadCounterTypeMap[type] = drawerItem
    }

    data class UnreadCounter(
            override var profileId: Int,
            override var type: Int,
            override var drawerItemId: Int?,
            override var count: Int
    ) : IUnreadCounter

    fun setUnreadCount(profileId: Int, type: Int, count: Int) {
        val item = unreadCounterList.singleOrNull {
            it.type == type && it.profileId == profileId
        }
        if (item != null) {
            item.count = count
        }
        else {
            unreadCounterList.add(UnreadCounter(profileId, type, null, count))
        }
        updateBadges()
    }
}
