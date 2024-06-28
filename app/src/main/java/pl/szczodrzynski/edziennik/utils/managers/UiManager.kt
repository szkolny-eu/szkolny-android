/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-26.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.Theme
import pl.szczodrzynski.edziennik.data.enums.Theme.Mode
import pl.szczodrzynski.edziennik.data.enums.Theme.Type
import pl.szczodrzynski.edziennik.ext.isNightMode
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.util.Locale

class UiManager(val app: App) {
    companion object {
        private const val TAG = "UiManager"
    }

    val themeColor
        get() = app.config.ui.themeColor

    fun applyNightMode() {
        val mode = app.config.ui.themeMode
        val nightMode = app.config.ui.themeNightMode

        if (mode == Mode.FULL)
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        else
            AppCompatDelegate.setDefaultNightMode(
                when (nightMode) {
                    true -> MODE_NIGHT_YES
                    false -> MODE_NIGHT_NO
                    null -> MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
    }

    fun applyTheme(activity: AppCompatActivity, noDisplay: Boolean = false) {
        val type = app.config.ui.themeType
        val mode = app.config.ui.themeMode
        val blackMode = app.config.ui.themeBlackMode

        val themeRes = if (noDisplay) {
            when (type) {
                Type.M2 -> R.style.AppTheme_M2_NoDisplay
                Type.M3 -> R.style.AppTheme_M3_NoDisplay
            }
        } else {
            var color = app.config.ui.themeColor
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                && color == Theme.DEFAULT
                && type == Type.M3
            ) {
                color = Theme.BLUE
            }
            d(TAG, "Applying theme $color($type, $mode)")
            color.styleRes[type to mode] ?: color.styleRes[Type.M3 to Mode.DAYNIGHT]!!
        }
        activity.setTheme(themeRes)

        if (!noDisplay && blackMode && activity.isNightMode)
            activity.theme?.applyStyle(R.style.ThemeOverlay_AppTheme_Black, true)
    }

    fun applyLanguage(context: Context) {
        val language = app.config.ui.language ?: return
        val locale = Locale(language.lowercase())
        val configuration = context.resources.configuration
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
        }
        configuration.locale = locale
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }
}
