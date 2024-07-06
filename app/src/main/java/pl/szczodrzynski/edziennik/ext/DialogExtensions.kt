/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-30.
 */

package pl.szczodrzynski.edziennik.ext

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialDialogs
import com.google.android.material.shape.MaterialShapeDrawable
import pl.szczodrzynski.edziennik.R


@SuppressLint("RestrictedApi")
fun AlertDialog.overlayBackgroundColor(color: Int, alpha: Int) {
    // this is absolutely horrible
    val colorSurface16dp = ColorUtils.compositeColors(
        R.color.colorSurface_16dp.resolveColor(context),
        MaterialColors.getColor(
            context,
            R.attr.colorSurface,
            javaClass.canonicalName,
        )
    )
    val colorDialogBackground = MaterialColors.layer(colorSurface16dp, color, alpha / 255f)
    val backgroundInsets = MaterialDialogs.getDialogBackgroundInsets(
        context,
        R.attr.alertDialogStyle,
        R.style.MaterialAlertDialog_Material3,
    )
    val background = MaterialShapeDrawable(
        context,
        null,
        R.attr.alertDialogStyle,
        R.style.MaterialAlertDialog_Material3
    )
    with(background) {
        initializeElevationOverlay(context)
        fillColor = ColorStateList.valueOf(colorDialogBackground)
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setCornerSize(android.R.attr.dialogCornerRadius.resolveDimenAttr(context))
        }*/
        elevation = ViewCompat.getElevation(window?.decorView ?: return@with)
    }
    val insetDrawable = MaterialDialogs.insetDrawable(background, backgroundInsets)
    window?.setBackgroundDrawable(insetDrawable)
}
