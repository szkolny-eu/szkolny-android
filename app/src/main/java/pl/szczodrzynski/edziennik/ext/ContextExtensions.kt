/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.getSystemService
import pl.szczodrzynski.edziennik.App

val Context.app
    get() = applicationContext as App

val Context.isNightMode: Boolean
    get() {
        if (getSystemService<UiModeManager>()?.nightMode == UiModeManager.MODE_NIGHT_YES)
            return true
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES) != 0
    }
