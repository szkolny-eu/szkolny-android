/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.view.View
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.db.enums.*
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem

fun Int.asFeatureType() = FeatureType.values().first { it.id == this }
fun Int.asLoginMethod() = LoginMethod.values().first { it.id == this }
fun Int.asLoginMode() = LoginMode.values().first { it.id == this }
fun Int.asLoginType() = LoginType.values().first { it.id == this }
fun Int.asMetadataType() = MetadataType.values().first { it.id == this }
fun Int.asNavTarget() = NavTarget.values().first { it.id == this }

fun Int?.asFeatureTypeOrNull() = FeatureType.values().firstOrNull { it.id == this }
fun Int?.asLoginMethodOrNull() = LoginMethod.values().firstOrNull { it.id == this }
fun Int?.asLoginModeOrNull() = LoginMode.values().firstOrNull { it.id == this }
fun Int?.asLoginTypeOrNull() = LoginType.values().firstOrNull { it.id == this }
fun Int?.asMetadataTypeOrNull() = MetadataType.values().firstOrNull { it.id == this }
fun Int?.asNavTargetOrNull() = NavTarget.values().firstOrNull { it.id == this }

fun Enum<*>.toInt() = when (this) {
    is FeatureType -> this.id
    is LoginMethod -> this.id
    is LoginMode -> this.id
    is LoginType -> this.id
    is MetadataType -> this.id
    is NavTarget -> this.id
    else -> throw IllegalArgumentException("Unknown enum type: $this")
}

inline fun <reified E> Int.toEnum() = when (E::class.java) {
    FeatureType::class.java -> this.asFeatureType()
    LoginMethod::class.java -> this.asLoginMethod()
    LoginMode::class.java -> this.asLoginMode()
    LoginType::class.java -> this.asLoginType()
    MetadataType::class.java -> this.asMetadataType()
    NavTarget::class.java -> this.asNavTarget()
    else -> throw IllegalArgumentException("Unknown enum type: ${E::class.java}")
} as E

fun getFeatureTypesNecessary() = FeatureType.values().filter { it.isAlwaysNeeded }

fun NavTarget.toBottomSheetItem(activity: MainActivity) =
    BottomSheetPrimaryItem(isContextual = false).also {
        it.titleRes = this.nameRes
        if (this.icon != null)
            it.iconicsIcon = this.icon
        it.onClickListener = View.OnClickListener {
            activity.navigate(navTarget = this)
        }
    }
