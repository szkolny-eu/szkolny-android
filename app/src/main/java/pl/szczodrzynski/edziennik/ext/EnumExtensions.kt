/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.data.enums.NotificationType
import pl.szczodrzynski.edziennik.data.enums.Theme

fun <E : Enum<E>> String.toEnum(type: Class<*>) = when (type) {
    FeatureType::class.java -> enumValueOf<FeatureType>(this)
    LoginMethod::class.java -> enumValueOf<LoginMethod>(this)
    LoginMode::class.java -> enumValueOf<LoginMode>(this)
    LoginType::class.java -> enumValueOf<LoginType>(this)
    MetadataType::class.java -> enumValueOf<MetadataType>(this)
    NavTarget::class.java -> enumValueOf<NavTarget>(this)
    NotificationType::class.java -> enumValueOf<NotificationType>(this)
    Theme::class.java -> enumValueOf<Theme>(this)
    else -> throw IllegalArgumentException("Unknown type $type")
} as E

fun <E : Enum<E>> String.toEnumOrNull(type: Class<*>): E? =
    try {
        this.toEnum<E>(type)
    } catch (e: Exception) {
        null
    }

inline fun <reified E : Enum<E>> String.toEnum(): E =
    this.toEnum(E::class.java)

inline fun <reified E : Enum<E>> String.toEnumOrNull(): E? =
    this.toEnumOrNull<E>(E::class.java)
