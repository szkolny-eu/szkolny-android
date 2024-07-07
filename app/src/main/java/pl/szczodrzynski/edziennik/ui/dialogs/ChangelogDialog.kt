/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-30.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ui.base.dialog.ViewDialog
import pl.szczodrzynski.edziennik.utils.BetterLinkMovementMethod
import pl.szczodrzynski.edziennik.utils.html.BetterHtml

class ChangelogDialog(
    activity: AppCompatActivity,
) : ViewDialog<ScrollView>(activity) {

    override fun getTitleRes() = R.string.whats_new
    override fun getPositiveButtonText() = R.string.close

    override fun getRootView(): ScrollView {
        val textView = TextView(activity)
        textView.setPadding(24.dp, 24.dp, 24.dp, 0)

        var text = app.assets.open("pl-changelog.html").bufferedReader().use {
            it.readText()
        }

        val commitsUrlPrefix = "https://github.com/szkolny-eu/szkolny-android/commits?author="
        text = text.replace(
            regex = """\[(.+?)]\(@([A-z0-9-]+)\)""".toRegex(),
            replacement = "<a href=\"$commitsUrlPrefix$2\">$1</a>"
        )
        text = text.replace(
            regex = """\s@([A-z0-9-]+)""".toRegex(),
            replacement = " <a href=\"$commitsUrlPrefix$1\">@$1</a>"
        )

        textView.text = BetterHtml.fromHtml(activity, text)

        textView.movementMethod = BetterLinkMovementMethod.getInstance()

        val scrollView = ScrollView(activity)
        scrollView.addView(textView)

        return scrollView
    }
}
