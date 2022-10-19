/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.getIntList
import pl.szczodrzynski.edziennik.config.utils.set
import pl.szczodrzynski.edziennik.ext.asNavTargetOrNull
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget

class ConfigUI(private val config: Config) {
    private var mTheme: Int? = null
    var theme: Int
        get() { mTheme = mTheme ?: config.values.get("theme", 1); return mTheme ?: 1 }
        set(value) { config.set("theme", value); mTheme = value }

    private var mLanguage: String? = null
    var language: String?
        get() { mLanguage = mLanguage ?: config.values.get("language", null as String?); return mLanguage }
        set(value) { config.set("language", value); mLanguage = value }

    private var mHeaderBackground: String? = null
    var headerBackground: String?
        get() { mHeaderBackground = mHeaderBackground ?: config.values.get("headerBg", null as String?); return mHeaderBackground }
        set(value) { config.set("headerBg", value); mHeaderBackground = value }

    private var mAppBackground: String? = null
    var appBackground: String?
        get() { mAppBackground = mAppBackground ?: config.values.get("appBg", null as String?); return mAppBackground }
        set(value) { config.set("appBg", value); mAppBackground = value }

    private var mMiniMenuVisible: Boolean? = null
    var miniMenuVisible: Boolean
        get() { mMiniMenuVisible = mMiniMenuVisible ?: config.values.get("miniMenuVisible", false); return mMiniMenuVisible ?: false }
        set(value) { config.set("miniMenuVisible", value); mMiniMenuVisible = value }

    private var mMiniMenuButtons: List<NavTarget>? = null
    var miniMenuButtons: List<NavTarget>
        get() { mMiniMenuButtons = mMiniMenuButtons ?: config.values.getIntList("miniMenuButtons", listOf())?.mapNotNull { it.asNavTargetOrNull() }; return mMiniMenuButtons ?: listOf() }
        set(value) { config.set("miniMenuButtons", value.map { it.id }); mMiniMenuButtons = value }

    private var mOpenDrawerOnBackPressed: Boolean? = null
    var openDrawerOnBackPressed: Boolean
        get() { mOpenDrawerOnBackPressed = mOpenDrawerOnBackPressed ?: config.values.get("openDrawerOnBackPressed", false); return mOpenDrawerOnBackPressed ?: false }
        set(value) { config.set("openDrawerOnBackPressed", value); mOpenDrawerOnBackPressed = value }

    private var mSnowfall: Boolean? = null
    var snowfall: Boolean
        get() { mSnowfall = mSnowfall ?: config.values.get("snowfall", false); return mSnowfall ?: false }
        set(value) { config.set("snowfall", value); mSnowfall = value }

    private var mEggfall: Boolean? = null
    var eggfall: Boolean
        get() { mEggfall = mEggfall ?: config.values.get("eggfall", false); return mEggfall ?: false }
        set(value) { config.set("eggfall", value); mEggfall = value }

    private var mBottomSheetOpened: Boolean? = null
    var bottomSheetOpened: Boolean
        get() { mBottomSheetOpened = mBottomSheetOpened ?: config.values.get("bottomSheetOpened", false); return mBottomSheetOpened ?: false }
        set(value) { config.set("bottomSheetOpened", value); mBottomSheetOpened = value }
}
