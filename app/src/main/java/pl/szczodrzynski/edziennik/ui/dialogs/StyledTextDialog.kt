/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-11.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.content.res.ColorStateList
import android.text.Editable
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.StyledTextDialogBinding
import pl.szczodrzynski.edziennik.utils.DefaultTextStyles
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.managers.TextStylingManager.HtmlMode.SIMPLE
import pl.szczodrzynski.edziennik.utils.managers.TextStylingManager.StylingConfig

class StyledTextDialog(
    val activity: AppCompatActivity,
    val initialText: Editable?,
    val onSuccess: (text: Editable) -> Unit,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "StyledTextDialog"
    }

    private lateinit var app: App
    private lateinit var b: StyledTextDialogBinding
    private lateinit var dialog: AlertDialog
    private lateinit var config: StylingConfig

    private val manager
        get() = app.textStylingManager

    init {
        show()
    }

    fun show() {
        if (activity.isFinishing)
            return
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        b = StyledTextDialogBinding.inflate(activity.layoutInflater)

        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.styled_text_dialog_title)
            .setView(b.root)
            .setPositiveButton(R.string.save) { _, _ ->
                onSuccess(b.editText.text ?: SpannableStringBuilder(""))
            }
            .setNeutralButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG)
            }
            .show()

        config = StylingConfig(
            editText = b.editText,
            fontStyleGroup = b.fontStyle.styles,
            fontStyleClear = b.fontStyle.clear,
            styles = DefaultTextStyles.getAsList(b.fontStyle),
            textHtml = null,
            htmlMode = SIMPLE,
        )

        manager.attach(config)

        b.editText.text = initialText

        if (Themes.isDark) {
            val colorStateList = ColorStateList.valueOf(0x40ffffff)
            b.fontStyle.bold.strokeColor = colorStateList
            b.fontStyle.italic.strokeColor = colorStateList
            b.fontStyle.underline.strokeColor = colorStateList
            b.fontStyle.strike.strokeColor = colorStateList
            b.fontStyle.subscript.strokeColor = colorStateList
            b.fontStyle.superscript.strokeColor = colorStateList
            b.fontStyle.clear.strokeColor = colorStateList
        }
    }
}

