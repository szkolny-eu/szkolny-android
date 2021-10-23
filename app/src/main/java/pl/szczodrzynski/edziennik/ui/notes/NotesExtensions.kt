/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-23.
 */

package pl.szczodrzynski.edziennik.ui.notes

import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.onClick

fun MaterialButton.setupNotesButton(
    activity: AppCompatActivity,
    profileId: Int,
    owner: Noteable,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) {
    icon = IconicsDrawable(activity, CommunityMaterial.Icon3.cmd_playlist_edit)
    setText(R.string.notes_button)
    iconPadding = 8.dp
    iconSize = 24.dp

    updateLayoutParams<LinearLayout.LayoutParams> {
        gravity = Gravity.CENTER_HORIZONTAL
    }
    updatePadding(left = 12.dp)

    onClick {
        NoteListDialog(
            activity = activity,
            profileId = profileId,
            owner = owner,
            onShowListener = onShowListener,
            onDismissListener = onDismissListener,
        ).show()
    }
}
