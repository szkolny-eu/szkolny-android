/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.template

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogTemplateBinding
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.ViewDialog

/**
 * This class represents a sample dialog using the new style.
 *
 * A dialog may subclass the [BaseDialog], [ViewDialog] or [BindingDialog].
 *
 * Fields and methods have the preferred order which should be used when writing new code.
 * The position of the first occurrence of duplicated methods should be used.
 * Multi-line methods should be followed by a blank line, one-liners may be just joined together.
 *
 * Constructor properties should be private.
 *
 * [onShow], when not used, should be placed just before the local variables, as a one-liner.
 * All other multi-line methods go below the local variables part.
 */
class TemplateDialog(
    activity: AppCompatActivity,
    private val onActionPerformed: (() -> Unit)? = null,
) : BindingDialog<DialogTemplateBinding>(activity) {

    override fun getTitleRes() = R.string.menu_template
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogTemplateBinding.inflate(layoutInflater)

    // override fun getTitle(): CharSequence = "Template"
    // override fun getTitleRes() = R.string.menu_template
    // override fun getMessage() = ""
    // override fun getMessageRes() = R.string.edziennik_progress_login_template_api
    // override fun getMessageFormat() =
    //     R.string.card_update_text_format to listOf(
    //         BuildConfig.VERSION_BASE,
    //         "5.0",
    //     )

    // override fun getTitleRes() = R.string.menu_template
    override fun isCancelable() = true
    override fun getPositiveButtonText() = R.string.ok
    override fun getNeutralButtonText() = R.string.reset
    override fun getNegativeButtonText() = R.string.cancel

    // getSingleChoiceItem / getMultiChoiceItems
    // getDefaultSelectedItem / getDefaultSelectedItems

    // to convert a map of StringIDs to CharSequences
    // .mapKeys { (resId, _) -> activity.getString(resId) }

    // local variables go here

    // onPositiveClick
    // onNeutralClick
    // onNegativeClick

    // onSingleSelectionChanged
    // onMultiSelectionChanged

    // getRootView
    // onBeforeShow
    // onShow
    // onDismiss
}
