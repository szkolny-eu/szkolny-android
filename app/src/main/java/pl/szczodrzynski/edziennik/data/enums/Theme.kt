/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-27.
 */

package pl.szczodrzynski.edziennik.data.enums

import pl.szczodrzynski.edziennik.R

enum class Theme(
    val nameRes: Int,
    val styleRes: Map<Pair<Type, Mode>, Int>,
) {
    DEFAULT(
        nameRes = R.string.theme_default,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3,
        ),
    ),
    RED(
        nameRes = R.string.theme_red,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Red,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Red_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Red,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Red_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Red_Classic,
        ),
    ),
    GREEN(
        nameRes = R.string.theme_green,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Green,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Green_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Green,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Green_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Green_Classic,
        ),
    ),
    BLUE(
        nameRes = R.string.theme_blue,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Blue,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Blue_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Blue,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Blue_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Blue_Classic,
        ),
    ),
    PURPLE(
        nameRes = R.string.theme_purple,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Purple,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Purple_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Purple,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Purple_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Purple_Classic,
        ),
    ),
    TEAL(
        nameRes = R.string.theme_teal,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Teal,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Teal_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Teal,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Teal_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Teal_Classic,
        ),
    );

    enum class Type {
        M2,
        M3,
    }

    enum class Mode {
        DAYNIGHT,
        FULL,
        CLASSIC,
    }
}
