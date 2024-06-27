/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import pl.szczodrzynski.edziennik.App

val Context.app
    get() = applicationContext as App
