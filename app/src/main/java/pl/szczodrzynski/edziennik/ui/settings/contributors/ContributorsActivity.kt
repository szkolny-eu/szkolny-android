package pl.szczodrzynski.edziennik.ui.settings.contributors

import android.os.Bundle
import android.os.Process
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ContributorsResponse
import pl.szczodrzynski.edziennik.databinding.ContributorsActivityBinding
import pl.szczodrzynski.edziennik.ui.base.lazypager.FragmentLazyPagerAdapter
import pl.szczodrzynski.edziennik.ui.error.ErrorSnackbar
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

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

    private var konami = 0

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> konami in 0..1
            KeyEvent.KEYCODE_DPAD_DOWN -> konami in 2..3
            KeyEvent.KEYCODE_DPAD_LEFT -> konami in 4..6 step 2
            KeyEvent.KEYCODE_DPAD_RIGHT -> konami in 5..7 step 2
            KeyEvent.KEYCODE_B -> konami == 8
            KeyEvent.KEYCODE_A -> konami == 9
            else -> false
        }.let {
            if (!it) {
                konami = 0
                return super.onKeyUp(keyCode, event)
            }
            konami++
            b.konami.isVisible = konami == 10
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as App
        b = ContributorsActivityBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.progressBar.isVisible = true
        b.tabLayout.isVisible = false
        b.viewPager.isVisible = false

        b.szkolny.onLongClick {
            if (b.konami.isVisible) {
                b.glove.isVisible = true
                b.szkolny.isInvisible = true
            }
            true
        }

        b.glove.onClick {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.dev_mode_enable_warning)
                .setPositiveButton(R.string.yes) { _, _ ->
                    app.config.devMode = true
                    App.devMode = true
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Restart")
                        .setMessage("Wymagany restart aplikacji")
                        .setPositiveButton(R.string.ok) { _, _ ->
                            Process.killProcess(Process.myPid())
                            Runtime.getRuntime().exit(0)
                            exitProcess(0)
                        }
                        .setCancelable(false)
                        .show()
                }
                .setNegativeButton(R.string.no) { _, _ ->
                    app.config.devMode = false
                    App.devMode = false
                    this.finish()
                }
                .show()
        }

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
