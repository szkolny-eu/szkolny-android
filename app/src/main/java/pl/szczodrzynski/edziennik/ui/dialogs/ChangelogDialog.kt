/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-30.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.os.Build
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
import pl.szczodrzynski.edziennik.utils.BetterLinkMovementMethod
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
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

        var text = app.assets.open("pl-changelog.html").bufferedReader().use {
            it.readText()
        }

        val commitsUrlPrefix = "https://github.com/szkolny-eu/szkolny-android/commits?author="
        text = text.replace("""\[(.+?)]\(@([A-z0-9-]+)\)""".toRegex(), "<a href=\"$commitsUrlPrefix$2\">$1</a>")
        text = text.replace("""\s@([A-z0-9-]+)""".toRegex(), " <a href=\"$commitsUrlPrefix$1\">@$1</a>")

        val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            text
        else
            text.replace("<li>", "<br><li> - ")
        textView.text = BetterHtml.fromHtml(activity, html)

        textView.movementMethod = BetterLinkMovementMethod.getInstance()

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
