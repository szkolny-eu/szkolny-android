package pl.szczodrzynski.edziennik.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.navlib.getColorFromAttr

object Themes {
    data class Theme(
            val id: Int,
            @StringRes val name: Int,
            @StyleRes val style: Int,
            val isDark: Boolean
    )

    val themeList = arrayListOf(
            Theme(id = 0, name = R.string.theme_light, style = R.style.AppTheme_Light, isDark = false),
            Theme(id = 1, name = R.string.theme_dark, style = R.style.AppTheme_Dark, isDark = true),
            Theme(id = 2, name = R.string.theme_black, style = R.style.AppTheme_Black, isDark = true),
            Theme(id = 3, name = R.string.theme_chocolate, style = R.style.AppTheme_Chocolate, isDark = true),
            Theme(id = 4, name = R.string.theme_indigo, style = R.style.AppTheme_Indigo, isDark = true),
            Theme(id = 5, name = R.string.theme_light_yellow, style = R.style.AppTheme_LightYellow, isDark = false),
            Theme(id = 6, name = R.string.theme_dark_blue, style = R.style.AppTheme_DarkBlue, isDark = true),
            Theme(id = 7, name = R.string.theme_blue, style = R.style.AppTheme_Blue, isDark = true),
            Theme(id = 8, name = R.string.theme_light_blue, style = R.style.AppTheme_LightBlue, isDark = false),
            Theme(id = 9, name = R.string.theme_dark_purple, style = R.style.AppTheme_DarkPurple, isDark = true),
            Theme(id = 10, name = R.string.theme_purple, style = R.style.AppTheme_Purple, isDark = true),
            Theme(id = 11, name = R.string.theme_light_purple, style = R.style.AppTheme_LightPurple, isDark = false),
            Theme(id = 12, name = R.string.theme_dark_red, style = R.style.AppTheme_DarkRed, isDark = true),
            Theme(id = 13, name = R.string.theme_red, style = R.style.AppTheme_Red, isDark = true),
            Theme(id = 14, name = R.string.theme_light_red, style = R.style.AppTheme_LightRed, isDark = false),
            Theme(id = 15, name = R.string.theme_dark_green, style = R.style.AppTheme_DarkGreen, isDark = true),
            Theme(id = 16, name = R.string.theme_amber, style = R.style.AppTheme_Amber, isDark = false),
            Theme(id = 17, name = R.string.theme_light_green, style = R.style.AppTheme_LightGreen, isDark = false)
    )

    var theme: Theme = themeList[1]
    var themeInt
        get() = theme.id
        set(value) {
            theme = themeList[value]
        }

    var themeIndex
        get() = themeList.indexOf(theme)
        set(value) {
            theme = themeList[value]
        }


    val appThemeNoDisplay: Int
        get() = if (theme.isDark) R.style.AppTheme_Dark_NoDisplay else R.style.AppTheme_Light_NoDisplay


    val appTheme: Int
        get() = theme.style

    val isDark: Boolean
        get() = theme.isDark

    fun getPrimaryTextColor(context: Context): Int {
        return getColorFromAttr(context, android.R.attr.textColorPrimary)
    }

    fun getSecondaryTextColor(context: Context): Int {
        return getColorFromAttr(context, android.R.attr.textColorSecondary)
    }

    fun getThemeName(context: Context): String {
        return context.getString(theme.name)
    }

    @StringRes
    fun getThemeNameRes() = theme.name

    fun getThemeNames(context: Context) =
        themeList.map {
            context.getString(it.name)
        }
}
