package pl.szczodrzynski.edziennik.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.navlib.getColorFromAttr

object Themes {

    fun getPrimaryTextColor(context: Context): Int {
        return getColorFromAttr(context, android.R.attr.textColorPrimary)
    }

    fun getSecondaryTextColor(context: Context): Int {
        return getColorFromAttr(context, android.R.attr.textColorSecondary)
    }
}
