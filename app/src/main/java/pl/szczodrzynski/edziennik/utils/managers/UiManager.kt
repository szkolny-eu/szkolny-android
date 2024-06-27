/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-26.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.Theme
import pl.szczodrzynski.edziennik.data.enums.Theme.BLUE
import pl.szczodrzynski.edziennik.data.enums.Theme.DEFAULT
import java.util.Locale

class UiManager(val app: App) {
    companion object {
        private const val TAG = "ThemeManager"
    }

    private val uiModeManager by lazy { app.getSystemService<UiModeManager>()!! }

    val themeColor
        get() = app.config.ui.themeConfig.color
    val themeType
        get() = app.config.ui.themeConfig.type
    val themeMode
        get() = app.config.ui.themeConfig.mode
    val themeNightMode
        get() = app.config.ui.themeConfig.nightMode

    private val themeStyleRes by lazy {
        val res = themeColor.styleRes[themeType to themeMode]
        if (res != null)
            return@lazy res
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return@lazy BLUE.styleRes[Theme.Type.M3 to Theme.Mode.DAYNIGHT]!!
        return@lazy DEFAULT.styleRes[Theme.Type.M3 to Theme.Mode.DAYNIGHT]!!
    }

    val isDark: Boolean
        get() {
            if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES)
                return true
            return (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES) != 0
        }

    fun applyTheme(activity: AppCompatActivity, noDisplay: Boolean = false) {
        if (themeMode == Theme.Mode.FULL)
            uiModeManager.nightMode = UiModeManager.MODE_NIGHT_YES
        else
            uiModeManager.nightMode = themeNightMode
        activity.setTheme(
            when {
                noDisplay && themeType == Theme.Type.M2 -> R.style.AppTheme_M2_NoDisplay
                noDisplay && themeType == Theme.Type.M3 -> R.style.AppTheme_M2_NoDisplay
                else -> themeStyleRes
            }
        )
    }

    fun applyStyle(context: Context) {
//        context?.theme?.applyStyle(appTheme, true)
    }

    fun getContextWrapper(context: Context) = ContextThemeWrapper(context, themeStyleRes)

    fun setLanguage(context: Context) {
        val language = app.config.ui.language ?: return
        val locale = Locale(language.lowercase())
        val configuration = app.resources.configuration
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
        }
        configuration.locale = locale
        app.resources.updateConfiguration(configuration, app.resources.displayMetrics)
    }
}
