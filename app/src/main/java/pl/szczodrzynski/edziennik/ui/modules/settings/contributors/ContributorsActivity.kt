package pl.szczodrzynski.edziennik.ui.modules.settings.contributors

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.Bundle
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ContributorsResponse
import pl.szczodrzynski.edziennik.databinding.ContributorsActivityBinding
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.FragmentLazyPagerAdapter
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar
import kotlin.coroutines.CoroutineContext

class ContributorsActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        private const val TAG = "ContributorsActivity"
        private var contributors: ContributorsResponse? = null
    }

    private lateinit var app: App
    private lateinit var b: ContributorsActivityBinding

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here
    private val errorSnackbar: ErrorSnackbar by lazy { ErrorSnackbar(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as App
        b = ContributorsActivityBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.progressBar.isVisible = true
        b.tabLayout.isVisible = false
        b.viewPager.isVisible = false

        launch {
            contributors = contributors ?: SzkolnyApi(app).runCatching(errorSnackbar) {
                getContributors()
            } ?: return@launch

            val pagerAdapter = FragmentLazyPagerAdapter(
                supportFragmentManager,
                fragments = listOf(
                    ContributorsFragment().apply {
                        arguments = Bundle(
                            "items" to contributors!!.contributors.toTypedArray(),
                            "quantityPluralRes" to R.plurals.contributions_quantity,
                        )
                    } to getString(R.string.contributors),

                    ContributorsFragment().apply {
                        arguments = Bundle(
                            "items" to contributors!!.translators.toTypedArray(),
                            "quantityPluralRes" to R.plurals.translations_quantity,
                        )
                    } to getString(R.string.translators),
                )
            )

            b.viewPager.apply {
                offscreenPageLimit = 1
                adapter = pagerAdapter
                b.tabLayout.setupWithViewPager(this)
            }

            b.progressBar.isVisible = false
            b.tabLayout.isVisible = true
            b.viewPager.isVisible = true
        }
    }
}
