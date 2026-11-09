package pl.szczodrzynski.edziennik.utils

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.ui.feedback.FeedbackActivity
import pl.szczodrzynski.edziennik.ui.login.LoginActivity
import pl.szczodrzynski.edziennik.ui.settings.contributors.ContributorsActivity

/**
 * Android 15 (API 35) enforces edge-to-edge for apps targeting it,
 * so the content is drawn behind the status and navigation bars.
 *
 * [MainActivity], [LoginActivity] and [FeedbackActivity] handle
 * the window insets themselves, [ContributorsActivity] uses
 * fitsSystemWindows. For all other (full-screen) activities this
 * pads the content view by the system bars and the keyboard.
 */
object EdgeToEdgeInsets : Application.ActivityLifecycleCallbacks {

    private val handledActivities = setOf(
        MainActivity::class.java,
        LoginActivity::class.java,
        FeedbackActivity::class.java,
        ContributorsActivity::class.java,
    )

    fun register(application: Application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
            application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity::class.java in handledActivities)
            return
        if (activity.isFloatingWindow())
            return
        val content = activity.findViewById<View>(android.R.id.content) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
                        or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    /**
     * Dialog-like activities are not drawn edge-to-edge.
     */
    private fun Activity.isFloatingWindow(): Boolean {
        val value = TypedValue()
        return theme.resolveAttribute(android.R.attr.windowIsFloating, value, true)
                && value.data != 0
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
