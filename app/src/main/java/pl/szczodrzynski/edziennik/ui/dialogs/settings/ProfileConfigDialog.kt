/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-23.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.content.res.ColorStateList
import android.view.LayoutInflater
import androidx.core.widget.addTextChangedListener
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.DialogProfileConfigBinding
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.dialogs.ProfileRemoveDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog

class ProfileConfigDialog(
    activity: MainActivity,
    private val profile: Profile,
    private val onProfileSaved: ((profile: Profile) -> Unit)? = null,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<DialogProfileConfigBinding>(activity, onShowListener, onDismissListener) {

    override val TAG = "ProfileConfigDialog"

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogProfileConfigBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close

    private var profileChanged = false
    private var profileRemoved = false

    override suspend fun onShow() {
        b.profile = profile
        profile.applyImageTo(b.image)

        // I can't believe how simple it is to get the dialog's background color !!
        val shape = MaterialShapeDrawable(
            activity,
            null,
            R.attr.alertDialogStyle,
            R.style.MaterialAlertDialog_Material3
        )
        val surface = MaterialColors.getColor(activity, R.attr.colorSurface, TAG)
        shape.setCornerSize(18.dp.toFloat())
        shape.initializeElevationOverlay(activity)
        shape.fillColor = ColorStateList.valueOf(surface)
        shape.elevation = 16.dp.toFloat()
        b.circleView.background = shape

        b.nameEdit.addTextChangedListener {
            profileChanged = true
        }

        b.syncSwitch.onChange { _, _ ->
            profileChanged = true
        }

        b.imageButton.onClick {
            if (activity !is MainActivity)
                return@onClick
            activity.requestHandler.requestProfileImage(profile) {
                val profile = it as? Profile ?: return@requestProfileImage
                if (this@ProfileConfigDialog.profile == profile) {
                    profileChanged = true
                    b.profile = profile
                    b.image.setImageDrawable(profile.getImageDrawable(activity))
                }
            }
        }

        b.logoutButton.onClick {
            ProfileRemoveDialog(activity, profile.id, profile.name) {
                profileRemoved = true
                dialog.dismiss()
            }.show()
        }
    }

    override fun onDismiss() {
        if (!profileRemoved && profileChanged) {
            app.profileSave(profile)
            onProfileSaved?.invoke(profile)
        }
    }
}
