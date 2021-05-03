package pl.szczodrzynski.edziennik.ui.modules.settings.contributors

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.databinding.LoginActivityBinding
import pl.szczodrzynski.edziennik.databinding.WebPushFragmentBinding
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar
import pl.szczodrzynski.edziennik.ui.modules.settings.contributors.adapters.ContributorsPagerAdapter
import kotlin.coroutines.CoroutineContext

class ContributorsActivity : AppCompatActivity(), CoroutineScope {

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    val activity = this

    val app: App by lazy {
        applicationContext as App
    }

    private val api by lazy {
        SzkolnyApi(app)
    }

    val errorSnackbar: ErrorSnackbar by lazy { ErrorSnackbar(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contributors)

        launch {
            val contributors = api.runCatching(errorSnackbar) {
                getContributors()
            }

            val fragmentAdapter = ContributorsPagerAdapter(supportFragmentManager, contributors)
            val viewPager = findViewById<ViewPager>(R.id.contributorsViewPager)
            val tabLayout = findViewById<TabLayout>(R.id.contributorsTabLayout)
            viewPager.adapter = fragmentAdapter

            tabLayout.setupWithViewPager(viewPager)
        }
    }
}
