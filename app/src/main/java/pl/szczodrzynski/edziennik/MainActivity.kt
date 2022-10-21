package pl.szczodrzynski.edziennik

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.navigation.NavOptions
import com.danimahardhika.cafebar.CafeBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jetradarmobile.snowfall.SnowfallView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.mikepenz.materialdrawer.model.*
import com.mikepenz.materialdrawer.model.interfaces.*
import com.mikepenz.materialdrawer.model.utils.hiddenInMiniDrawer
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.droidsonroids.gif.GifDrawable
import pl.szczodrzynski.edziennik.data.api.ERROR_VULCAN_API_DEPRECATED
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.*
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Metadata.*
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.databinding.ActivitySzkolnyBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.sync.AppManagerDetectedEvent
import pl.szczodrzynski.edziennik.sync.SyncWorker
import pl.szczodrzynski.edziennik.sync.UpdateWorker
import pl.szczodrzynski.edziennik.ui.base.MainSnackbar
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.base.enums.NavTargetLocation
import pl.szczodrzynski.edziennik.ui.dialogs.ChangelogDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.ProfileConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.sync.RegisterUnavailableDialog
import pl.szczodrzynski.edziennik.ui.dialogs.sync.ServerMessageDialog
import pl.szczodrzynski.edziennik.ui.dialogs.sync.SyncViewListDialog
import pl.szczodrzynski.edziennik.ui.dialogs.sync.UpdateAvailableDialog
import pl.szczodrzynski.edziennik.ui.error.ErrorDetailsDialog
import pl.szczodrzynski.edziennik.ui.error.ErrorSnackbar
import pl.szczodrzynski.edziennik.ui.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.login.LoginActivity
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesFragment
import pl.szczodrzynski.edziennik.ui.timetable.TimetableFragment
import pl.szczodrzynski.edziennik.utils.*
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.Utils.dpToPx
import pl.szczodrzynski.edziennik.utils.managers.AvailabilityManager.Error.Type
import pl.szczodrzynski.edziennik.utils.managers.UserActionManager
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.*
import pl.szczodrzynski.navlib.SystemBarsUtil.Companion.COLOR_HALF_TRANSPARENT
import pl.szczodrzynski.navlib.bottomsheet.NavBottomSheet
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import pl.szczodrzynski.navlib.drawer.NavDrawer
import pl.szczodrzynski.navlib.drawer.items.DrawerPrimaryItem
import java.io.IOException
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    val b: ActivitySzkolnyBinding by lazy { ActivitySzkolnyBinding.inflate(layoutInflater) }
    val navView: NavView by lazy { b.navView }
    val drawer: NavDrawer by lazy { navView.drawer }
    val bottomSheet: NavBottomSheet by lazy { navView.bottomSheet }
    val mainSnackbar: MainSnackbar by lazy { MainSnackbar(this) }
    val errorSnackbar: ErrorSnackbar by lazy { ErrorSnackbar(this) }
    val requestHandler by lazy { MainActivityRequestHandler(this) }

    val swipeRefreshLayout: SwipeRefreshLayoutNoTouch by lazy { b.swipeRefreshLayout }

    var onBeforeNavigate: (() -> Boolean)? = null
    private var pausedNavigationData: PausedNavigationData? = null

    val app: App by lazy {
        applicationContext as App
    }

    private val fragmentManager by lazy { supportFragmentManager }
    lateinit var navTarget: NavTarget
        private set
    private var navArguments: Bundle? = null

    private val navBackStack = mutableListOf<Pair<NavTarget, Bundle?>>()
    private var navLoading = true

    /*     ____           _____                _
          / __ \         / ____|              | |
         | |  | |_ __   | |     _ __ ___  __ _| |_ ___
         | |  | | '_ \  | |    | '__/ _ \/ _` | __/ _ \
         | |__| | | | | | |____| | |  __/ (_| | ||  __/
          \____/|_| |_|  \_____|_|  \___|\__,_|\__\__*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        d(TAG, "Activity created")

        setTheme(Themes.appTheme)

        app.config.ui.language?.let {
            setLanguage(it)
        }

        app.buildManager.validateBuild(this)

        if (App.profileId == 0) {
            onProfileListEmptyEvent(ProfileListEmptyEvent())
            return
        }

        d(TAG, "Profile is valid, inflating views")

        setContentView(b.root)

        mainSnackbar.setCoordinator(b.navView.coordinator, b.navView.bottomBar)
        errorSnackbar.setCoordinator(b.navView.coordinator, b.navView.bottomBar)

        val versionBadge = app.buildManager.versionBadge
        b.nightlyText.isVisible = versionBadge != null
        b.nightlyText.text = versionBadge
        if (versionBadge != null) {
            b.nightlyText.background.setTintColor(0xa0ff0000.toInt())
        }

        navLoading = true

        b.navView.apply {
            drawer.init(this@MainActivity)

            SystemBarsUtil(this@MainActivity).run {
                //paddingByKeyboard = b.navView
                appFullscreen = false
                statusBarColor = getColorFromAttr(context, android.R.attr.colorBackground)
                statusBarDarker = false
                statusBarFallbackLight = COLOR_HALF_TRANSPARENT
                statusBarFallbackGradient = COLOR_HALF_TRANSPARENT
                navigationBarTransparent = false

                b.navView.configSystemBarsUtil(this)

                // fix for setting status bar color to window color, outside of navlib
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.statusBarColor = statusBarColor
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && ColorUtils.calculateLuminance(statusBarColor) > 0.6
                ) {
                    @Suppress("deprecation")
                    window.decorView.systemUiVisibility =
                        window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }

                // TODO fix navlib navbar detection, orientation change issues, status bar color setting if not fullscreen

                commit()
            }

            toolbar.apply {
                subtitleFormat = R.string.toolbar_subtitle
                subtitleFormatWithUnread = R.plurals.toolbar_subtitle_with_unread
            }

            bottomBar.apply {
                fabEnable = false
                fabExtendable = true
                fabExtended = false
                fabGravity = Gravity.CENTER
                if (Themes.isDark) {
                    setBackgroundColor(blendColors(
                        getColorFromAttr(context, R.attr.colorSurface),
                        getColorFromRes(R.color.colorSurface_4dp)
                    ))
                    elevation = dpToPx(4).toFloat()
                }
            }

            bottomSheet.apply {
                removeAllItems()
                toggleGroupEnabled = false
                textInputEnabled = false
                onCloseListener = {
                    if (!app.config.ui.bottomSheetOpened)
                        app.config.ui.bottomSheetOpened = true
                }
            }

            drawer.apply {
                setAccountHeaderBackground(app.config.ui.headerBackground)

                drawerProfileListEmptyListener = {
                    onProfileListEmptyEvent(ProfileListEmptyEvent())
                }
                drawerItemSelectedListener = { id, _, item ->
                    if (item is ExpandableDrawerItem)
                        false
                    else
                        navigate(navTarget = id.asNavTargetOrNull())
                }
                drawerProfileSelectedListener = { id, _, _, _ ->
                    // why is this negated -_-
                    !navigate(profileId = id)
                }
                drawerProfileLongClickListener = { _, profile, _, view ->
                    if (view != null && profile is ProfileDrawerItem) {
                        launch {
                            val appProfile = withContext(Dispatchers.IO) {
                                App.db.profileDao().getByIdNow(profile.identifier.toInt())
                            } ?: return@launch
                            drawer.close()
                            ProfileConfigDialog(this@MainActivity, appProfile).show()
                        }
                        true
                    } else {
                        false
                    }
                }
                drawerProfileImageLongClickListener = drawerProfileLongClickListener
                drawerProfileSettingClickListener = this@MainActivity.profileSettingClickListener

                miniDrawerVisibleLandscape = null
                miniDrawerVisiblePortrait = app.config.ui.miniMenuVisible
            }
        }

        navTarget = NavTarget.HOME

        if (savedInstanceState != null) {
            intent?.putExtras(savedInstanceState)
            savedInstanceState.clear()
        }

        app.db.profileDao().all.observe(this) { profiles ->
            val allArchived = profiles.all { it.archived }
            drawer.setProfileList(profiles.filter {
                it.id >= 0 && (!it.archived || allArchived)
            }.toMutableList())
            //prepend the archived profile if loaded
            if (app.profile.archived && !allArchived) {
                drawer.prependProfile(Profile(
                    id = app.profile.id,
                    loginStoreId = app.profile.loginStoreId,
                    loginStoreType = app.profile.loginStoreType,
                    name = app.profile.name,
                    subname = "Archiwum - ${app.profile.subname}"
                ).also {
                    it.archived = true
                })
            }
            drawer.currentProfile = App.profileId
        }

        setDrawerItems()

        handleIntent(intent?.extras)

        app.db.metadataDao().unreadCounts.observe(this) { unreadCounters ->
            drawer.setUnreadCounterList(unreadCounters)
        }

        b.swipeRefreshLayout.isEnabled = true
        b.swipeRefreshLayout.setOnRefreshListener { launch { syncCurrentFeature() } }
        b.swipeRefreshLayout.setColorSchemeResources(
            R.color.md_blue_500,
            R.color.md_amber_500,
            R.color.md_green_500
        )

        SyncWorker.scheduleNext(app)
        UpdateWorker.scheduleNext(app)

        // if loaded profile is archived, switch to the up-to-date version of it
        if (app.profile.archived) {
            launch {
                if (app.profile.archiveId != null) {
                    val profile = withContext(Dispatchers.IO) {
                        app.db.profileDao().getNotArchivedOf(app.profile.archiveId!!)
                    }
                    if (profile != null)
                        navigate(profile = profile)
                    else
                        navigate(profileId = 0)
                } else {
                    navigate(profileId = 0)
                }
            }
        }

        // APP BACKGROUND
        setAppBackground()

        // IT'S WINTER MY DUDES
        val today = Date.getToday()
        if ((today.month % 11 == 1) && app.config.ui.snowfall) {
            b.rootFrame.addView(layoutInflater.inflate(R.layout.snowfall, b.rootFrame, false))
        } else if (app.config.ui.eggfall && BigNightUtil().isDataWielkanocyNearDzisiaj()) {
            val eggfall = layoutInflater.inflate(
                R.layout.eggfall,
                b.rootFrame,
                false
            ) as SnowfallView
            eggfall.setSnowflakeBitmaps(listOf(
                BitmapFactory.decodeResource(resources, R.drawable.egg1),
                BitmapFactory.decodeResource(resources, R.drawable.egg2),
                BitmapFactory.decodeResource(resources, R.drawable.egg3),
                BitmapFactory.decodeResource(resources, R.drawable.egg4),
                BitmapFactory.decodeResource(resources, R.drawable.egg5),
                BitmapFactory.decodeResource(resources, R.drawable.egg6)
            ))
            b.rootFrame.addView(eggfall)
        }

        // WHAT'S NEW DIALOG
        if (app.config.appVersion < BuildConfig.VERSION_CODE) {
            // force an AppSync after update
            app.config.sync.lastAppSync = 0L
            ChangelogDialog(this).show()
            if (app.config.appVersion < 170) {
                //Intent intent = new Intent(this, ChangelogIntroActivity.class);
                //startActivity(intent);
            } else {
                app.config.appVersion = BuildConfig.VERSION_CODE
            }
        }

        // RATE SNACKBAR
        if (app.config.appRateSnackbarTime != 0L && app.config.appRateSnackbarTime <= System.currentTimeMillis()) {
            navView.coordinator.postDelayed({
                CafeBar.builder(this)
                    .content(R.string.rate_snackbar_text)
                    .icon(IconicsDrawable(this).apply {
                        icon = CommunityMaterial.Icon3.cmd_star_outline
                        sizeDp = 24
                        colorInt = Themes.getPrimaryTextColor(this@MainActivity)
                    })
                    .positiveText(R.string.rate_snackbar_positive)
                    .positiveColor(-0xb350b0)
                    .negativeText(R.string.rate_snackbar_negative)
                    .negativeColor(0xff666666.toInt())
                    .neutralText(R.string.rate_snackbar_neutral)
                    .neutralColor(0xff666666.toInt())
                    .onPositive { cafeBar ->
                        Utils.openGooglePlay(this)
                        cafeBar.dismiss()
                        app.config.appRateSnackbarTime = 0
                    }
                    .onNegative { cafeBar ->
                        Toast.makeText(this,
                            R.string.rate_snackbar_negative_message,
                            Toast.LENGTH_LONG).show()
                        cafeBar.dismiss()
                        app.config.appRateSnackbarTime = 0
                    }
                    .onNeutral { cafeBar ->
                        Toast.makeText(this, R.string.ok, Toast.LENGTH_LONG).show()
                        cafeBar.dismiss()
                        app.config.appRateSnackbarTime =
                            System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000
                    }
                    .autoDismiss(false)
                    .swipeToDismiss(true)
                    .floating(true)
                    .show()
            }, 10000)
        }

        // CONTEXT MENU ITEMS
        bottomSheet.removeAllItems()
        bottomSheet.appendItems(
            BottomSheetPrimaryItem(false)
                .withTitle(R.string.menu_sync)
                .withIcon(CommunityMaterial.Icon.cmd_download_outline)
                .withOnClickListener {
                    bottomSheet.close()
                    SyncViewListDialog(this, navTarget).show()
                },
            BottomSheetSeparatorItem(false),
        )
        for (target in NavTarget.values()) {
            if (target.location != NavTargetLocation.BOTTOM_SHEET)
                continue
            if (target.devModeOnly && !App.devMode)
                continue
            bottomSheet += target.toBottomSheetItem(this)
        }
    }

    private var profileSettingClickListener = { itemId: Int, _: View? ->
        when (val item = itemId.asNavTarget()) {
            NavTarget.PROFILE_ADD -> {
                requestHandler.requestLogin()
            }
            NavTarget.PROFILE_SYNC_ALL -> {
                EdziennikTask.sync().enqueue(this)
            }
            NavTarget.PROFILE_MARK_AS_READ -> {
                launch {
                    withContext(Dispatchers.Default) {
                        app.db.profileDao().allNow.forEach { profile ->
                            if (profile.loginStoreType != LoginType.LIBRUS)
                                app.db.metadataDao()
                                    .setAllSeenExceptMessagesAndAnnouncements(profile.id, true)
                            else
                                app.db.metadataDao().setAllSeenExceptMessages(profile.id, true)
                        }
                    }
                    Toast.makeText(this@MainActivity,
                        R.string.main_menu_mark_as_read_success,
                        Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                navigate(navTarget = item)
            }
        }
        false
    }

    /*     _____
          / ____|
         | (___  _   _ _ __   ___
          \___ \| | | | '_ \ / __|
          ____) | |_| | | | | (__
         |_____/ \__, |_| |_|\___|
                  __/ |
                 |__*/
    private suspend fun syncCurrentFeature() {
        if (app.profile.archived) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_archived_title)
                .setMessage(
                    R.string.profile_archived_text,
                    app.profile.studentSchoolYearStart,
                    app.profile.studentSchoolYearStart + 1
                )
                .setPositiveButton(R.string.ok, null)
                .show()
            swipeRefreshLayout.isRefreshing = false
            return
        }
        if (app.profile.shouldArchive()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_archiving_title)
                .setMessage(
                    R.string.profile_archiving_format,
                    app.profile.dateYearEnd.formattedString
                )
                .setPositiveButton(R.string.ok, null)
                .show()
        }
        if (app.profile.isBeforeYear()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_year_not_started_title)
                .setMessage(
                    R.string.profile_year_not_started_format,
                    app.profile.dateSemester1Start.formattedString
                )
                .setPositiveButton(R.string.ok, null)
                .show()
            swipeRefreshLayout.isRefreshing = false
            return
        }

        val error = withContext(Dispatchers.IO) {
            app.availabilityManager.check(app.profile)
        }
        when (error?.type) {
            Type.NOT_AVAILABLE -> {
                swipeRefreshLayout.isRefreshing = false
                navigate(navTarget = NavTarget.HOME)
                RegisterUnavailableDialog(this, error.status!!).show()
                return
            }
            Type.API_ERROR -> {
                errorSnackbar.addError(error.apiError!!).show()
                return
            }
            Type.NO_API_ACCESS -> {
                Toast.makeText(this, R.string.error_no_api_access, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }

        swipeRefreshLayout.isRefreshing = true
        Toast.makeText(this, fragmentToSyncName(navTarget), Toast.LENGTH_SHORT).show()
        val featureType = when (navTarget) {
            NavTarget.MESSAGES -> when (MessagesFragment.pageSelection) {
                Message.TYPE_SENT -> FeatureType.MESSAGES_SENT
                else -> FeatureType.MESSAGES_INBOX
            }
            else -> navTarget.featureType
        }
        val arguments = when (navTarget) {
            NavTarget.TIMETABLE -> JsonObject("weekStart" to TimetableFragment.pageSelection?.weekStart?.stringY_m_d)
            else -> null
        }
        EdziennikTask.syncProfile(
            App.profileId,
            featureType?.let { setOf(it) },
            arguments = arguments
        ).enqueue(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onUpdateEvent(event: Update) {
        EventBus.getDefault().removeStickyEvent(event)
        UpdateAvailableDialog(this, event).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onRegisterAvailabilityEvent(event: RegisterAvailabilityEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        val error = app.availabilityManager.check(app.profile, cacheOnly = true)
        if (error != null) {
            RegisterUnavailableDialog(this, error.status!!).show()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskStartedEvent(event: ApiTaskStartedEvent) {
        swipeRefreshLayout.isRefreshing = true
        if (event.profileId == App.profileId) {
            navView.toolbar.apply {
                subtitleFormat = null
                subtitleFormatWithUnread = null
                subtitle = getString(R.string.toolbar_subtitle_syncing)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProfileListEmptyEvent(event: ProfileListEmptyEvent) {
        d(TAG, "Profile list is empty. Launch LoginActivity.")
        app.config.loginFinished = false
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTaskProgressEvent(event: ApiTaskProgressEvent) {
        if (event.profileId == App.profileId) {
            navView.toolbar.apply {
                subtitleFormat = null
                subtitleFormatWithUnread = null
                subtitle = if (event.progress < 0f)
                    event.progressText ?: ""
                else
                    getString(
                        R.string.toolbar_subtitle_syncing_format,
                        event.progress.roundToInt(),
                        event.progressText ?: "",
                    )

            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onApiTaskFinishedEvent(event: ApiTaskFinishedEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.profileId == App.profileId) {
            navView.toolbar.apply {
                subtitleFormat = R.string.toolbar_subtitle
                subtitleFormatWithUnread = R.plurals.toolbar_subtitle_with_unread
                subtitle = "Gotowe"
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onApiTaskAllFinishedEvent(event: ApiTaskAllFinishedEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        swipeRefreshLayout.isRefreshing = false
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onApiTaskErrorEvent(event: ApiTaskErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.error.errorCode == ERROR_VULCAN_API_DEPRECATED) {
            if (event.error.profileId != App.profileId)
                return
            ErrorDetailsDialog(this, listOf(event.error)).show()
        }
        navView.toolbar.apply {
            subtitleFormat = R.string.toolbar_subtitle
            subtitleFormatWithUnread = R.plurals.toolbar_subtitle_with_unread
            subtitle = "Gotowe"
        }
        mainSnackbar.dismiss()
        errorSnackbar.addError(event.error).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAppManagerDetectedEvent(event: AppManagerDetectedEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        if (app.config.sync.dontShowAppManagerDialog)
            return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_manager_dialog_title)
            .setMessage(R.string.app_manager_dialog_text)
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    for (intent in appManagerIntentList) {
                        if (packageManager.resolveActivity(intent,
                                PackageManager.MATCH_DEFAULT_ONLY) != null
                        ) {
                            startActivity(intent)
                        }
                    }
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, R.string.app_manager_open_failed, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            .setNeutralButton(R.string.dont_ask_again) { _, _ ->
                app.config.sync.dontShowAppManagerDialog = true
            }
            .setCancelable(false)
            .show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserActionRequiredEvent(event: UserActionRequiredEvent) {
        app.userActionManager.execute(this, event, UserActionManager.UserActionCallback())
    }

    private fun fragmentToSyncName(navTarget: NavTarget): Int {
        return when (navTarget) {
            NavTarget.TIMETABLE -> R.string.sync_feature_timetable
            NavTarget.AGENDA -> R.string.sync_feature_agenda
            NavTarget.GRADES -> R.string.sync_feature_grades
            NavTarget.HOMEWORK -> R.string.sync_feature_homework
            NavTarget.BEHAVIOUR -> R.string.sync_feature_notices
            NavTarget.ATTENDANCE -> R.string.sync_feature_attendance
            NavTarget.MESSAGES -> when (MessagesFragment.pageSelection) {
                Message.TYPE_SENT -> R.string.sync_feature_messages_outbox
                else -> R.string.sync_feature_messages_inbox
            }
            NavTarget.ANNOUNCEMENTS -> R.string.sync_feature_announcements
            else -> R.string.sync_feature_syncing_all
        }
    }

    /*    _____       _             _
         |_   _|     | |           | |
           | |  _ __ | |_ ___ _ __ | |_ ___
           | | | '_ \| __/ _ \ '_ \| __/ __|
          _| |_| | | | ||  __/ | | | |_\__ \
         |_____|_| |_|\__\___|_| |_|\__|__*/
    private val intentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleIntent(intent?.extras)
        }
    }

    fun handleIntent(extras: Bundle?) {
        d(TAG, "handleIntent() {")
        extras?.keySet()?.forEach { key ->
            d(TAG, "    \"$key\": " + extras.get(key))
        }
        d(TAG, "}")

        val intentProfileId = extras.getIntOrNull("profileId").takePositive()
        var intentNavTarget = extras.getIntOrNull("fragmentId").asNavTargetOrNull()

        if (extras?.containsKey("action") == true) {
            val handled = when (extras.getString("action")) {
                "serverMessage" -> {
                    ServerMessageDialog(
                        this,
                        extras.getString("serverMessageTitle") ?: getString(R.string.app_name),
                        extras.getString("serverMessageText") ?: ""
                    ).show()
                    true
                }
                "feedbackMessage" -> {
                    intentNavTarget = NavTarget.FEEDBACK
                    false
                }
                "userActionRequired" -> {
                    val event = UserActionRequiredEvent(
                        profileId = extras.getInt("profileId"),
                        type = extras.getEnum<UserActionRequiredEvent.Type>("type") ?: return,
                        params = extras.getBundle("params") ?: return,
                        errorText = 0,
                    )
                    app.userActionManager.execute(this,
                        event,
                        UserActionManager.UserActionCallback())
                    true
                }
                "createManualEvent" -> {
                    val date = extras.getString("eventDate")
                        ?.let { Date.fromY_m_d(it) }
                        ?: Date.getToday()
                    EventManualDialog(
                        this,
                        App.profileId,
                        defaultDate = date
                    ).show()
                    true
                }
                else -> false
            }
            if (handled && !navLoading) {
                return
            }
        }

        if (extras?.containsKey("reloadProfileId") == true) {
            val reloadProfileId = extras.getIntOrNull("reloadProfileId").takePositive()
            if (reloadProfileId == null || app.profile.id == reloadProfileId) {
                reloadTarget()
                return
            }
        }

        extras?.remove("profileId")
        extras?.remove("fragmentId")
        extras?.remove("reloadProfileId")

        /*if (intentTargetId == -1 && navController.currentDestination?.id == R.id.loadingFragment) {
            intentTargetId = navTarget.id
        }*/

        if (navLoading)
            b.fragment.removeAllViews()

        when {
            app.profile.id == 0 -> navigate(
                profileId = intentProfileId ?: app.config.lastProfileId,
                navTarget = intentNavTarget,
                args = extras,
            )
            intentProfileId != null -> navigate(
                profileId = intentProfileId,
                navTarget = intentNavTarget,
                args = extras,
            )
            intentNavTarget != null -> navigate(
                navTarget = intentNavTarget,
                args = extras,
            )
            navLoading -> navigate()
            else -> drawer.currentProfile = app.profile.id
        }
        navLoading = false
    }

    override fun recreate() {
        recreate(navTarget)
    }

    fun recreate(navTarget: NavTarget) {
        recreate(navTarget, null)
    }

    fun recreate(navTarget: NavTarget? = null, arguments: Bundle? = null) {
        val intent = Intent(this, MainActivity::class.java)
        if (arguments != null)
            intent.putExtras(arguments)
        if (navTarget != null) {
            intent.putExtra("fragmentId", navTarget.id)
        }
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        startActivity(intent)
    }

    override fun onStart() {
        d(TAG, "Activity started")
        super.onStart()
    }

    override fun onStop() {
        d(TAG, "Activity stopped")
        super.onStop()
    }

    override fun onResume() {
        d(TAG, "Activity resumed")
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_MAIN)
        registerReceiver(intentReceiver, filter)
        EventBus.getDefault().register(this)
        super.onResume()
    }

    override fun onPause() {
        d(TAG, "Activity paused")
        unregisterReceiver(intentReceiver)
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    override fun onDestroy() {
        d(TAG, "Activity destroyed")
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putExtras("fragmentId" to navTarget)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent?.extras)
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        requestHandler.handleResult(requestCode, resultCode, data)
    }

    /*    _                     _                  _   _               _
         | |                   | |                | | | |             | |
         | |     ___   __ _  __| |  _ __ ___   ___| |_| |__   ___   __| |___
         | |    / _ \ / _` |/ _` | | '_ ` _ \ / _ \ __| '_ \ / _ \ / _` / __|
         | |___| (_) | (_| | (_| | | | | | | |  __/ |_| | | | (_) | (_| \__ \
         |______\___/ \__,_|\__,_| |_| |_| |_|\___|\__|_| |_|\___/ \__,_|__*/
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.task_open_enter) // new fragment enter
        .setExitAnim(R.anim.task_open_exit) // old fragment exit
        .setPopEnterAnim(R.anim.task_close_enter) // old fragment enter back
        .setPopExitAnim(R.anim.task_close_exit) // new fragment exit
        .build()

    private fun canNavigate(): Boolean = onBeforeNavigate?.invoke() != false

    fun resumePausedNavigation(): Boolean {
        val data = pausedNavigationData ?: return false
        navigate(
            profileId = data.profileId,
            navTarget = data.navTarget,
            args = data.args,
            skipBeforeNavigate = true,
        )
        pausedNavigationData = null
        return true
    }

    fun navigate(
        profileId: Int? = null,
        profile: Profile? = null,
        navTarget: NavTarget? = null,
        args: Bundle? = null,
        skipBeforeNavigate: Boolean = false,
    ): Boolean {
        d(TAG, "navigate(profileId = ${profile?.id ?: profileId}, target = ${navTarget?.name}, args = $args)")
        if (!(skipBeforeNavigate || navTarget == this.navTarget) && !canNavigate()) {
            bottomSheet.close()
            drawer.close()
            // restore the previous profile if changing it with the drawer
            // well, it still does not change the toolbar profile image,
            // but that's now NavView's problem, not mine.
            drawer.currentProfile = App.profile.id
            pausedNavigationData = PausedNavigationData(profileId, navTarget, args)
            return false
        }

        val loadNavTarget = navTarget ?: this.navTarget
        if (profile != null && profile.id != App.profileId) {
            navigateImpl(profile, loadNavTarget, args, profileChanged = true)
            return true
        }
        if (profileId != null && profileId != App.profileId) {
            app.profileLoad(profileId) {
                navigateImpl(it, loadNavTarget, args, profileChanged = true)
            }
            return true
        }
        navigateImpl(App.profile, loadNavTarget, args, profileChanged = false)
        return true
    }

    private fun navigateImpl(
        profile: Profile,
        navTarget: NavTarget,
        args: Bundle?,
        profileChanged: Boolean,
    ) {
        d(TAG, "navigateImpl(profileId = ${profile.id}, target = ${navTarget.name}, args = $args)")

        if (navTarget.featureType != null && !profile.hasUIFeature(navTarget.featureType)) {
            navigateImpl(profile, NavTarget.HOME, args, profileChanged)
            return
        }

        if (profileChanged) {
            app.profileLoad(profile)
            MessagesFragment.pageSelection = -1
            // set new drawer items for this profile
            setDrawerItems()

            val previousArchivedId = if (app.profile.archived) app.profile.id else null
            if (previousArchivedId != null) {
                // prevents accidentally removing the first item if the archived profile is not shown
                drawer.removeProfileById(previousArchivedId)
            }
            if (profile.archived) {
                // add the same profile but with a different name
                // (other fields are not needed by the drawer)
                drawer.prependProfile(Profile(
                    id = profile.id,
                    loginStoreId = profile.loginStoreId,
                    loginStoreType = profile.loginStoreType,
                    name = profile.name,
                    subname = "Archiwum - ${profile.subname}"
                ).also {
                    it.archived = true
                })
            }

            // the drawer profile is updated automatically when the drawer item is clicked
            // update it manually when switching profiles from other source
            //if (drawer.currentProfile != app.profile.id)
            drawer.currentProfile = App.profileId
        }

        val arguments = args
            ?: navBackStack.firstOrNull { it.first == navTarget }?.second
            ?: Bundle()
        bottomSheet.close()
        bottomSheet.removeAllContextual()
        bottomSheet.toggleGroupEnabled = false
        drawer.close()
        if (drawer.getSelection() != navTarget.id)
            drawer.setSelection(navTarget.id, fireOnClick = false)
        navView.toolbar.setTitle(navTarget.titleRes ?: navTarget.nameRes)
        navView.bottomBar.fabEnable = false
        navView.bottomBar.fabExtended = false
        navView.bottomBar.setFabOnClickListener(null)

        d("NavDebug", "Navigating from ${this.navTarget.name} to ${navTarget.name}")

        val fragment = navTarget.fragmentClass?.newInstance() ?: return
        fragment.arguments = arguments
        val transaction = fragmentManager.beginTransaction()

        if (navTarget == this.navTarget) {
            // just reload the current target
            transaction.setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
        } else {
            navBackStack.keys().lastIndexOf(navTarget).let {
                if (it == -1)
                    return@let navTarget
                // pop the back stack up until that target
                transaction.setCustomAnimations(
                    R.anim.task_close_enter,
                    R.anim.task_close_exit
                )

                // navigating grades_add -> grades
                // navTarget == grades_add
                // navBackStack = [home, grades, grades_editor]
                // it == 1
                //
                // navTarget = target
                // remove 1
                // remove 2
                val popCount = navBackStack.size - it
                for (i in 0 until popCount) {
                    navBackStack.removeAt(navBackStack.lastIndex)
                }
                this.navTarget = navTarget
                this.navArguments = arguments

                return@let null
            }?.let {
                // target is neither current nor in the back stack
                // so navigate to it
                transaction.setCustomAnimations(
                    R.anim.task_open_enter,
                    R.anim.task_open_exit
                )
                navBackStack.add(this.navTarget to this.navArguments)
                this.navTarget = navTarget
                this.navArguments = arguments
            }
        }

        if (navTarget.popTo == NavTarget.HOME) {
            // if the current has popToHome, let only home be in the back stack
            // probably `if (navTarget.popToHome)` in popBackStack() is not needed now
            val popCount = navBackStack.size - 1
            for (i in 0 until popCount) {
                navBackStack.removeAt(navBackStack.lastIndex)
            }
        }

        d("NavDebug", "Current fragment ${navTarget.name}, back stack:")
        navBackStack.forEachIndexed { index, item ->
            d("NavDebug", " - $index: ${item.first.name}")
        }

        transaction.replace(R.id.fragment, fragment)
        transaction.commitAllowingStateLoss()

        // TASK DESCRIPTION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

            @Suppress("deprecation")
            val taskDesc = ActivityManager.TaskDescription(
                if (navTarget == NavTarget.HOME)
                    getString(R.string.app_name)
                else
                    getString(R.string.app_task_format, getString(navTarget.nameRes)),
                bm,
                getColorFromAttr(this, R.attr.colorSurface)
            )
            setTaskDescription(taskDesc)
        }
        return
    }

    fun reloadTarget() = navigate()

    private fun popBackStack(skipBeforeNavigate: Boolean = false): Boolean {
        if (navBackStack.size == 0) {
            return false
        }
        // TODO back stack argument support
        if (navTarget.popTo != null) {
            navigate(
                navTarget = navTarget.popTo,
                skipBeforeNavigate = skipBeforeNavigate,
            )
        } else {
            navBackStack.last().let {
                navigate(
                    navTarget = it.first,
                    args = it.second,
                    skipBeforeNavigate = skipBeforeNavigate,
                )
            }
        }
        return true
    }

    fun navigateUp(skipBeforeNavigate: Boolean = false) {
        if (!popBackStack(skipBeforeNavigate)) {
            super.onBackPressed()
        }
    }

    /**
     * Use the NavLib's menu button ripple to gain user attention
     * that something has changed in the bottom sheet.
     */
    fun gainAttention() {
        if (app.config.ui.bottomSheetOpened)
            return
        b.navView.postDelayed({
            navView.gainAttentionOnBottomBar()
        }, 2000)
    }

    fun gainAttentionFAB() {
        navView.bottomBar.fabExtended = false

        b.navView.postDelayed({
            navView.bottomBar.fabExtended = true
        }, 1000)

        b.navView.postDelayed({
            navView.bottomBar.fabExtended = false
        }, 3000)
    }

    fun setAppBackground() {
        try {
            b.root.background = app.config.ui.appBackground?.let {
                if (it.endsWith(".gif"))
                    GifDrawable(it)
                else
                    BitmapDrawable.createFromPath(it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /*    _____                                _ _
         |  __ \                              (_) |
         | |  | |_ __ __ ___      _____ _ __   _| |_ ___ _ __ ___  ___
         | |  | | '__/ _` \ \ /\ / / _ \ '__| | | __/ _ \ '_ ` _ \/ __|
         | |__| | | | (_| |\ V  V /  __/ |    | | ||  __/ | | | | \__ \
         |_____/|_|  \__,_| \_/\_/ \___|_|    |_|\__\___|_| |_| |_|__*/
    private fun createDrawerItem(target: NavTarget, level: Int = 1): IDrawerItem<*> {
        val item = when {
            // target.subItems != null -> ExpandableDrawerItem()
            level > 1 -> SecondaryDrawerItem()
            else -> DrawerPrimaryItem()
        }

        item.also {
            it.identifier = target.id.toLong()
            it.nameRes = target.nameRes
            it.descriptionRes = target.descriptionRes ?: -1
            it.icon = target.icon?.toImageHolder()
            it.hiddenInMiniDrawer = !app.config.ui.miniMenuButtons.contains(target)
            if (it is DrawerPrimaryItem)
                it.appTitle = target.titleRes?.resolveString(this)
            if (/* it is ColorfulBadgeable && */ target.badgeType != null)
                it.badgeStyle = drawer.badgeStyle
            it.isSelectedBackgroundAnimated = false
            it.level = level
        }
        if (target.badgeType != null)
            drawer.addUnreadCounterType(target.badgeType.id, target.id)

        /* item.subItems = target.subItems?.map {
            createDrawerItem(it, level + 1)
        }?.toMutableList() ?: mutableListOf() */

        return item
    }

    fun setDrawerItems() {
        d("NavDebug", "setDrawerItems() app.profile = ${app.profile}")
        val drawerItems = arrayListOf<IDrawerItem<*>>()
        val drawerItemsMore = arrayListOf<IDrawerItem<*>>()
        val drawerItemsBottom = arrayListOf<IDrawerItem<*>>()
        val drawerProfiles = arrayListOf<ProfileSettingDrawerItem>()

        for (target in NavTarget.values()) {
            if (target.devModeOnly && !App.devMode)
                continue
            if (target.featureType != null && !app.profile.hasUIFeature(target.featureType))
                continue

            when (target.location) {
                NavTargetLocation.DRAWER -> {
                    drawerItems += createDrawerItem(target, level = 1)
                }
                NavTargetLocation.DRAWER_MORE -> {
                    drawerItemsMore += createDrawerItem(target, level = 2)
                }
                NavTargetLocation.DRAWER_BOTTOM -> {
                    drawerItemsBottom += createDrawerItem(target, level = 1)
                }
                NavTargetLocation.PROFILE_LIST -> {
                    drawerProfiles += ProfileSettingDrawerItem().also {
                        it.identifier = target.id.toLong()
                        it.nameRes = target.nameRes
                        it.descriptionRes = target.descriptionRes ?: -1
                        it.icon = target.icon?.toImageHolder()
                    }
                }
                else -> continue
            }
        }

        drawerItems += ExpandableDrawerItem().also {
            it.identifier = -1L
            it.nameRes = R.string.menu_more
            it.icon = CommunityMaterial.Icon.cmd_dots_horizontal.toImageHolder()
            it.subItems = drawerItemsMore.toMutableList()
            it.isSelectedBackgroundAnimated = false
            it.isSelectable = false
        }
        drawerItems += DividerDrawerItem()
        drawerItems += drawerItemsBottom

        // seems that this cannot be open, because the itemAdapter has Profile items
        // instead of normal Drawer items...
        drawer.profileSelectionClose()
        drawer.setItems(*drawerItems.toTypedArray())
        drawer.removeAllProfileSettings()
        drawer.addProfileSettings(*drawerProfiles.toTypedArray())
    }

    override fun onBackPressed() {
        if (App.config.ui.openDrawerOnBackPressed) {
            if (drawer.isOpen)
                navigateUp()
            else if (!navView.onBackPressed())
                drawer.open()
        } else {
            if (!navView.onBackPressed())
                navigateUp()
        }
    }

    fun error(error: ApiError) = errorSnackbar.addError(error).show()
    fun snackbar(
        text: String,
        actionText: String? = null,
        onClick: (() -> Unit)? = null,
    ) = mainSnackbar.snackbar(text, actionText, onClick)

    fun snackbarDismiss() = mainSnackbar.dismiss()
}
