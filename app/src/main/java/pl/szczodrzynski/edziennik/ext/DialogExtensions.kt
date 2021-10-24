/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-30.
 */

package pl.szczodrzynski.edziennik.ext

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Build
import android.text.InputType
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.textfield.TextInputEditText
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogEditTextBinding

fun MaterialAlertDialogBuilder.input(
    message: CharSequence? = null,
    type: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
    hint: CharSequence? = null,
    value: CharSequence? = null,
    changeListener: ((editText: TextInputEditText, input: String) -> Boolean)? = null,
    positiveButton: Int? = null,
    positiveListener: ((editText: TextInputEditText, input: String) -> Boolean)? = null,
): MaterialAlertDialogBuilder {
    val b = DialogEditTextBinding.inflate(LayoutInflater.from(context), null, false)
    b.title.text = message
    b.title.isVisible = message.isNotNullNorBlank()
    b.text1.hint = hint
    b.text1.inputType = type
    b.text1.setText(value)
    b.text1.addTextChangedListener { text ->
        if (changeListener?.invoke(b.text1, text?.toString() ?: "") != false)
            b.text1.error = null
    }
    if (positiveButton != null) {
        setPositiveButton(positiveButton) { dialog, _ ->
            if (positiveListener?.invoke(b.text1, b.text1.text?.toString() ?: "") != false)
                dialog.dismiss()
        }
    }
    setView(b.root)

    return this
}

fun MaterialAlertDialogBuilder.setTitle(
    @StringRes resId: Int,
    vararg formatArgs: Any,
): MaterialAlertDialogBuilder {
    setTitle(context.getString(resId, *formatArgs))
    return this
}

fun MaterialAlertDialogBuilder.setMessage(
    @StringRes resId: Int,
    vararg formatArgs: Any,
): MaterialAlertDialogBuilder {
    setMessage(context.getString(resId, *formatArgs))
    return this
}

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
        R.style.MaterialAlertDialog_MaterialComponents,
    )
    val background = MaterialShapeDrawable(
        context,
        null,
        R.attr.alertDialogStyle,
        R.style.MaterialAlertDialog_MaterialComponents
    )
    with(background) {
        initializeElevationOverlay(context)
        fillColor = ColorStateList.valueOf(colorDialogBackground)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setCornerSize(android.R.attr.dialogCornerRadius.resolveDimenAttr(context))
        }
        elevation = ViewCompat.getElevation(window?.decorView ?: return@with)
    }
    val insetDrawable = MaterialDialogs.insetDrawable(background, backgroundInsets)
    window?.setBackgroundDrawable(insetDrawable)
}
