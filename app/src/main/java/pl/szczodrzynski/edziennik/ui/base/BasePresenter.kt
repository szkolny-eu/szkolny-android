/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.ui.base

open class BasePresenter<T : BaseView> {

    var view: T? = null

    open fun onAttachView(view: T) {
        this.view = view
    }

    open fun onDetachView() {
        view = null
    }
}
