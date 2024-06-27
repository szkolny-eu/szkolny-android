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

fun Int.asFeatureType() = FeatureType.entries.first { it.id == this }
fun Int.asLoginMethod() = LoginMethod.entries.first { it.id == this }
fun Int.asLoginMode() = LoginMode.entries.first { it.id == this }
fun Int.asLoginType() = LoginType.entries.first { it.id == this }
fun Int.asMetadataType() = MetadataType.entries.first { it.id == this }
fun Int.asNavTarget() = NavTarget.entries.first { it.id == this }
fun Int.asNotificationType() = NotificationType.entries.first { it.id == this }
fun Int.asTheme() = Theme.entries.first { it.id == this }

fun Int?.asFeatureTypeOrNull() = FeatureType.entries.firstOrNull { it.id == this }
fun Int?.asLoginMethodOrNull() = LoginMethod.entries.firstOrNull { it.id == this }
fun Int?.asLoginModeOrNull() = LoginMode.entries.firstOrNull { it.id == this }
fun Int?.asLoginTypeOrNull() = LoginType.entries.firstOrNull { it.id == this }
fun Int?.asMetadataTypeOrNull() = MetadataType.entries.firstOrNull { it.id == this }
fun Int?.asNavTargetOrNull() = NavTarget.entries.firstOrNull { it.id == this }
fun Int?.asNotificationTypeOrNull() = NotificationType.entries.firstOrNull { it.id == this }
fun Int?.asThemeOrNull() = Theme.entries.firstOrNull { it.id == this }

fun Enum<*>.toInt() = when (this) {
    is FeatureType -> this.id
    is LoginMethod -> this.id
    is LoginMode -> this.id
    is LoginType -> this.id
    is MetadataType -> this.id
    is NavTarget -> this.id
    is NotificationType -> this.id
    is Theme -> this.id
    else -> this.ordinal
}

inline fun <reified E : Enum<E>> Int.toEnum() = when (E::class.java) {
    // enums commented out are not really used in Bundles
    FeatureType::class.java -> this.asFeatureType()
    // LoginMethod::class.java -> this.asLoginMethod()
    LoginMode::class.java -> this.asLoginMode()
    LoginType::class.java -> this.asLoginType()
    // MetadataType::class.java -> this.asMetadataType()
    NavTarget::class.java -> this.asNavTarget()
    // NotificationType::class.java -> this.asNotificationType()
    // Theme::class.java -> this.asTheme()
    else -> enumValues<E>()[this]
} as E

fun <E : Enum<E>> Int.toEnum(type: Class<*>) = when (type) {
    // this is used for Config so all enums are here
    FeatureType::class.java -> this.asFeatureType()
    LoginMethod::class.java -> this.asLoginMethod()
    LoginMode::class.java -> this.asLoginMode()
    LoginType::class.java -> this.asLoginType()
    MetadataType::class.java -> this.asMetadataType()
    NavTarget::class.java -> this.asNavTarget()
    NotificationType::class.java -> this.asNotificationType()
    Theme::class.java -> this.asTheme()
    else -> throw IllegalArgumentException("Unknown type $type")
} as E
