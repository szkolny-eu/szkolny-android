/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-23.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.profile

import android.content.res.ColorStateList
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.DialogProfileConfigBinding
import kotlin.coroutines.CoroutineContext

class ProfileConfigDialog(
    val activity: MainActivity,
    val profile: Profile,
    val onProfileSaved: ((profile: Profile) -> Unit)? = null,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "ProfileConfigDialog"
    }

    private lateinit var app: App
    private lateinit var b: DialogProfileConfigBinding
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local variables go here
    private var profileChanged = false
    private var profileRemoved = false

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        b = DialogProfileConfigBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
            .setView(b.root)
            .setPositiveButton(R.string.close, null)
            .setOnDismissListener {
                if (!profileRemoved && profileChanged) {
                    app.profileSave(profile)
                    onProfileSaved?.invoke(profile)
                }
                onDismissListener?.invoke(TAG)
            }
            .show()

        b.profile = profile
        profile.applyImageTo(b.image)

        // I can't believe how simple it is to get the dialog's background color !!
        val shape = MaterialShapeDrawable(activity, null, R.attr.alertDialogStyle, R.style.MaterialAlertDialog_MaterialComponents)
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
            }
        }
    }}
}
