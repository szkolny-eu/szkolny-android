/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.ui.base

import android.widget.Toast
import androidx.fragment.app.Fragment

abstract class BaseFragment<T : BasePresenter<out BaseView>> : Fragment(), BaseView {

    abstract val presenter: T

    override fun showMessage(text: String) {
        Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDetachView()
    }
}
