package pl.szczodrzynski.edziennik.ui.modules.settings.contributors

import android.os.Bundle
import android.os.Process
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.FragmentLazyPagerAdapter
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar
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

    var konami = 0

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (konami == 0 || konami == 1)
                    konami += 1
                else
                    konami = 0
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (konami == 2 || konami == 3)
                    konami += 1
                else
                    konami = 0
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT-> {
                if (konami == 4 || konami == 6)
                    konami += 1
                else
                    konami = 0
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (konami == 5 || konami == 7)
                    konami += 1
                else
                    konami = 0
                true
            }
            KeyEvent.KEYCODE_B -> {
                if (konami == 8)
                    konami += 1
                else
                    konami = 0
                true
            }
            KeyEvent.KEYCODE_A -> {
                if (konami == 9) {
                    konami += 1
                    b.pickaxe.isVisible = true
                }
                else
                    konami = 0
                true
            }
            else -> {
                konami = 0
                super.onKeyUp(keyCode, event)
            }
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
            if (b.pickaxe.isVisible) {
                b.glove.isVisible = true
                b.szkolny.isVisible = false
            }
            true
        }

        b.glove.onClick {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.dev_mode_enable_warning)
                .setPositiveButton(R.string.yes) { _, _ ->
                    app.config.debugMode = true
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
                    app.config.debugMode = false
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
