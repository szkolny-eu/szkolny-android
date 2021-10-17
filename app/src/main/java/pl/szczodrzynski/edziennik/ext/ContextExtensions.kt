/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import android.os.Build
import java.util.*

fun Context.setLanguage(language: String) {
    val locale = Locale(language.toLowerCase(Locale.ROOT))
    val configuration = resources.configuration
    Locale.setDefault(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        configuration.setLocale(locale)
    }
    configuration.locale = locale
    resources.updateConfiguration(configuration, resources.displayMetrics)
}

/*
  Code copied from android-28/java.util.Locale.initDefault()
 */
fun initDefaultLocale() {
    run {
        // user.locale gets priority
        /*val languageTag: String? = System.getProperty("user.locale", "")
        if (languageTag.isNotNullNorEmpty()) {
            return@run Locale(languageTag)
        }*/

        // user.locale is empty
        val language: String? = System.getProperty("user.language", "pl")
        val region: String? = System.getProperty("user.region")
        val country: String?
        val variant: String?
        // for compatibility, check for old user.region property
        if (region != null) {
            // region can be of form country, country_variant, or _variant
            val i = region.indexOf('_')
            if (i >= 0) {
                country = region.substring(0, i)
                variant = region.substring(i + 1)
            } else {
                country = region
                variant = ""
            }
        } else {
            country = System.getProperty("user.country", "")
            variant = System.getProperty("user.variant", "")
        }
        return@run Locale(language)
    }.let {
        Locale.setDefault(it)
    }
}
