/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-24.
 */

package pl.szczodrzynski.edziennik.ui.notes

import android.content.DialogInterface.BUTTON_POSITIVE
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.core.manager.TextStylingManager.HtmlMode
import pl.szczodrzynski.edziennik.core.manager.TextStylingManager.StylingConfigBase
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.NoteEditorDialogBinding
import pl.szczodrzynski.edziennik.ext.isNotNullNorBlank
import pl.szczodrzynski.edziennik.ext.resolveString
import pl.szczodrzynski.edziennik.ext.toDrawable
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.RegistrationConfigDialog
import pl.szczodrzynski.edziennik.utils.TextInputDropDown

class NoteEditorDialog(
    activity: AppCompatActivity,
    private val owner: Noteable?,
    private val editingNote: Note?,
    private val profileId: Int =
        owner?.getNoteOwnerProfileId()
            ?: editingNote?.profileId
            ?: 0,
) : BindingDialog<NoteEditorDialogBinding>(activity) {

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        NoteEditorDialogBinding.inflate(layoutInflater)

    override fun isCancelable() = false
    override fun getPositiveButtonText() = R.string.save
    override fun getNeutralButtonText() = if (editingNote != null) R.string.remove else null
    override fun getNegativeButtonText() = R.string.cancel

    private lateinit var topicStylingConfig: StylingConfigBase
    private lateinit var bodyStylingConfig: StylingConfigBase
    private val manager
        get() = app.noteManager
    private val textStylingManager
        get() = app.textStylingManager

    private var progressDialog: BaseDialog<*>? = null

    override suspend fun onPositiveClick(): Boolean {
        val profile = withContext(Dispatchers.IO) {
            app.db.profileDao().getByIdNow(profileId)
        } ?: return NO_DISMISS

        val note = buildNote(profile) ?: return NO_DISMISS

        if (note.isShared && !profile.canShare) {
            RegistrationConfigDialog(activity, profile, onChangeListener = { enabled ->
                if (enabled)
                    onPositiveClick()
            }).showNoteShareDialog()
            return NO_DISMISS
        }

        if (note.isShared || editingNote?.isShared == true) {
            progressDialog = SimpleDialog<Unit>(activity) {
                title(R.string.please_wait)
                message(
                    when (note.isShared) {
                        true -> R.string.notes_editor_progress_sharing
                        false -> R.string.notes_editor_progress_unsharing
                    }
                )
                cancelable(false)
            }.show()
        }

        val success = manager.saveNote(
            activity = activity,
            note = note,
            teamId = owner?.getNoteShareTeamId(),
            wasShared = editingNote?.isShared ?: false,
        )
        progressDialog?.dismiss()
        return success
    }

    override suspend fun onNeutralClick(): Boolean {
        // editingNote cannot be null, as the button is visible

        val confirmation = SimpleDialog<Unit>(activity) {
            title(R.string.are_you_sure)
            message(R.string.notes_editor_confirmation_text)
            positive(R.string.yes)
            negative(R.string.no)
        }.showModal().getButton()
        if (confirmation != BUTTON_POSITIVE)
            return NO_DISMISS

        if (editingNote?.isShared == true) {
            progressDialog = SimpleDialog<Unit>(activity) {
                title(R.string.please_wait)
                message(R.string.notes_editor_progress_unsharing)
                cancelable(false)
            }.show()
        }

        val success = manager.deleteNote(activity, editingNote ?: return NO_DISMISS)
        progressDialog?.dismiss()
        return success
    }

    override suspend fun onShow() {
        manager.configureHeader(activity, owner, b.header)

        topicStylingConfig = StylingConfigBase(editText = b.topic, htmlMode = HtmlMode.SIMPLE)
        bodyStylingConfig = StylingConfigBase(editText = b.body, htmlMode = HtmlMode.SIMPLE)

        val profile = withContext(Dispatchers.IO) {
            app.db.profileDao().getByIdNow(profileId)
        }

        b.ownerType = owner?.getNoteType() ?: Note.OwnerType.NONE
        b.editingNote = editingNote
        b.shareByDefault = app.profile.config.shareByDefault && profile?.canShare == true

        b.color.clear().append(Note.Color.values().map { color ->
            TextInputDropDown.Item(
                id = color.value ?: 0L,
                text = color.stringRes.resolveString(activity),
                tag = color,
                icon = if (color.value != null)
                    CommunityMaterial.Icon.cmd_circle.toDrawable(color.value.toInt())
                else null,
            )
        })
        b.color.select(id = editingNote?.color ?: 0L)

        textStylingManager.attachToField(
            activity = activity,
            textLayout = b.topicLayout,
            textEdit = b.topic,
        )
        textStylingManager.attachToField(
            activity = activity,
            textLayout = b.bodyLayout,
            textEdit = b.body,
        )
    }

    private fun buildNote(profile: Profile): Note? {
        val ownerType = owner?.getNoteType() ?: Note.OwnerType.NONE
        val topic = b.topic.text?.toString()
        val body = b.body.text?.toString()
        val color = b.color.selected?.tag as? Note.Color

        val share = b.shareSwitch.isChecked && ownerType.isShareable
        val replace = b.replaceSwitch.isChecked && ownerType.canReplace

        if (body.isNullOrBlank()) {
            b.bodyLayout.error = app.getString(R.string.notes_editor_body_error)
            b.body.requestFocus()
            return null
        }

        val topicHtml = if (topic.isNotNullNorBlank())
            textStylingManager.getHtmlText(topicStylingConfig)
        else null
        val bodyHtml = textStylingManager.getHtmlText(bodyStylingConfig)

        return Note(
            profileId = profile.id,
            id = editingNote?.id ?: System.currentTimeMillis(),
            ownerType = owner?.getNoteType(),
            ownerId = owner?.getNoteOwnerId(),
            replacesOriginal = replace,
            topic = topicHtml,
            body = bodyHtml,
            color = color?.value,
            sharedBy = if (share) "self" else null,
            sharedByName = if (share) profile.studentNameLong else null,
        )
    }
}
