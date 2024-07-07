package pl.szczodrzynski.edziennik.ui.settings.contributors

import android.os.Bundle
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ContributorsResponse
import pl.szczodrzynski.edziennik.databinding.ContributorsFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.onLongClick
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.base.fragment.PagerFragment
import pl.szczodrzynski.edziennik.ui.dialogs.DevModeDialog
import pl.szczodrzynski.edziennik.ui.dialogs.RestartDialog

class ContributorsFragment : PagerFragment<ContributorsFragmentBinding, MainActivity>(
    inflater = ContributorsFragmentBinding::inflate,
) {
    companion object {
        private var contributors: ContributorsResponse? = null
    }

    override fun getTabLayout() = b.tabLayout
    override fun getViewPager() = b.viewPager
    override suspend fun onCreatePages() = listOf(
        ContributorsListFragment().apply {
            arguments = Bundle(
                "items" to contributors!!.contributors.toTypedArray(),
                "quantityPluralRes" to R.plurals.contributions_quantity,
            )
        } to getString(R.string.contributors),
        ContributorsListFragment().apply {
            arguments = Bundle(
                "items" to contributors!!.translators.toTypedArray(),
                "quantityPluralRes" to R.plurals.translations_quantity,
            )
        } to getString(R.string.translators),
    )

    // eggs
    /*private var konami = 0

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
    }*/

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.progressBar.isVisible = true
        b.tabLayout.isVisible = false
        b.viewPager.isVisible = false

        // eggs
        b.szkolny.onLongClick {
            if (b.konami.isVisible) {
                b.glove.isVisible = true
                b.szkolny.isInvisible = true
            }
            true
        }
        b.glove.onClick {
            DevModeDialog(activity).show()
        }

        contributors = contributors ?: SzkolnyApi(app).runCatching(activity.errorSnackbar) {
            getContributors()
        } ?: return

        b.progressBar.isVisible = false
        b.tabLayout.isVisible = true
        b.viewPager.isVisible = true

        super.onViewReady(savedInstanceState)
    }
}
