package pl.szczodrzynski.edziennik.ui.feedback

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.app

class FeedbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.uiManager.applyTheme(this)
        setContentView(R.layout.activity_feedback)

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
