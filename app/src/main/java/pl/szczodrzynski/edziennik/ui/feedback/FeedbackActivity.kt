package pl.szczodrzynski.edziennik.ui.feedback

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.utils.Themes.appTheme

class FeedbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(appTheme)
        setContentView(R.layout.activity_feedback)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(
                findViewById<FrameLayout>(R.id.feedbackFragment)
            ) { view: View, insets: WindowInsetsCompat ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.feedbackFragment, FeedbackFragment())
        transaction.commitAllowingStateLoss()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }
}
