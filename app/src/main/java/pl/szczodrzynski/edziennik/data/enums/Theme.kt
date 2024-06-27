/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-27.
 */

package pl.szczodrzynski.edziennik.data.enums

import android.app.UiModeManager
import android.os.Build
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R

enum class Theme(
    val id: Int,
    val nameRes: Int,
    val styleRes: Map<Pair<Type, Mode>, Int>,
) {
    DEFAULT(
        id = 0,
        nameRes = R.string.theme_default,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2,
            (Type.M2 to Mode.BLACK) to R.style.AppTheme_M2_Black,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3,
            (Type.M3 to Mode.BLACK) to R.style.AppTheme_M3_Black,
        ),
    ),
    RED(
        id = 1,
        nameRes = R.string.theme_red,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Red,
            (Type.M2 to Mode.BLACK) to R.style.AppTheme_M2_Red_Black,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Red_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Red,
            (Type.M3 to Mode.BLACK) to R.style.AppTheme_M3_Red_Black,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Red_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Red_Classic,
        ),
    ),
    GREEN(
        id = 2,
        nameRes = R.string.theme_green,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Green,
            (Type.M2 to Mode.BLACK) to R.style.AppTheme_M2_Green_Black,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Green_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Green,
            (Type.M3 to Mode.BLACK) to R.style.AppTheme_M3_Green_Black,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Green_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Green_Classic,
        ),
    ),
    BLUE(
        id = 3,
        nameRes = R.string.theme_blue,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Blue,
            (Type.M2 to Mode.BLACK) to R.style.AppTheme_M2_Blue_Black,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Blue_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Blue,
            (Type.M3 to Mode.BLACK) to R.style.AppTheme_M3_Blue_Black,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Blue_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Blue_Classic,
        ),
    ),
    PURPLE(
        id = 4,
        nameRes = R.string.theme_purple,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Purple,
            (Type.M2 to Mode.BLACK) to R.style.AppTheme_M2_Purple_Black,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Purple_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Purple,
            (Type.M3 to Mode.BLACK) to R.style.AppTheme_M3_Purple_Black,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Purple_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Purple_Classic,
        ),
    ),
    TEAL(
        id = 5,
        nameRes = R.string.theme_teal,
        styleRes = mapOf(
            (Type.M2 to Mode.DAYNIGHT) to R.style.AppTheme_M2_Teal,
            (Type.M2 to Mode.BLACK) to R.style.AppTheme_M2_Teal_Black,
            (Type.M2 to Mode.FULL) to R.style.AppTheme_M2_Teal_Full,
            (Type.M3 to Mode.DAYNIGHT) to R.style.AppTheme_M3_Teal,
            (Type.M3 to Mode.BLACK) to R.style.AppTheme_M3_Teal_Black,
            (Type.M3 to Mode.FULL) to R.style.AppTheme_M3_Teal_Full,
            (Type.M3 to Mode.CLASSIC) to R.style.AppTheme_M3_Teal_Classic,
        ),
    );

    enum class Type(id: Int) {
        M2(id = 1),
        M3(id = 2),
    }

    enum class Mode(id: Int) {
        DAYNIGHT(id = 1),
        BLACK(id = 2),
        FULL(id = 3),
        CLASSIC(id = 4),
    }

    data class Config(
        val color: Theme = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) BLUE else DEFAULT,
        val type: Type = Type.M3,
        val mode: Mode = Mode.DAYNIGHT,
        val nightMode: Int = UiModeManager.MODE_NIGHT_AUTO,
    )

    data class Override(
        val color: Theme? = null,
        val type: Type? = null,
        val mode: Mode? = null,
        val nightMode: Int? = null,
    )
}
