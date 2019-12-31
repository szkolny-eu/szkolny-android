/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-30.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.changelog

import android.text.Html
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.dp
import kotlin.coroutines.CoroutineContext

class ChangelogDialog(
        val activity: AppCompatActivity,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "ChangelogDialog"
    }

    private lateinit var app: App
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        val textView = TextView(activity)
        textView.setPadding(24.dp, 24.dp, 24.dp, 0)

        val text = app.assets.open("pl-changelog.html").bufferedReader().use {
            it.readText()
        }
        textView.text = Html.fromHtml(text)

        val scrollView = ScrollView(activity)
        scrollView.addView(textView)

        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.whats_new)
                .setView(scrollView)
                .setPositiveButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()
    }}
}