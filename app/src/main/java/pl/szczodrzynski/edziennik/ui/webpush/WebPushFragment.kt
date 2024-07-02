/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-19.
 */

package pl.szczodrzynski.edziennik.ui.webpush

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.szkolny.response.WebPushResponse
import pl.szczodrzynski.edziennik.databinding.WebPushFragmentBinding
import pl.szczodrzynski.edziennik.ext.crc32
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.QrScannerDialog
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class WebPushFragment : BaseFragment<WebPushFragmentBinding, MainActivity>(
    inflater = WebPushFragmentBinding::inflate,
) {

    private lateinit var adapter: WebPushBrowserAdapter
    private val api by lazy {
        SzkolnyApi(app)
    }

    private val manager
        get() = app.permissionManager

    override suspend fun onViewCreated(savedInstanceState: Bundle?) {
        b.scanQrCode.onClick {
            manager.requestCameraPermission(activity, R.string.permissions_qr_scanner) {
                QrScannerDialog(activity, {
                    b.tokenEditText.setText(it.crc32().toString(36).uppercase())
                    pairBrowser(browserId = it)
                }).show()
            }
        }

        b.tokenAccept.onClick {
            val pairToken = b.tokenEditText.text.toString().uppercase()
            if (!"[0-9A-Z]{3,13}".toRegex().matches(pairToken)) {
                b.tokenLayout.error = app.getString(R.string.web_push_token_invalid)
                return@onClick
            }
            b.tokenLayout.error = null
            b.tokenEditText.setText(pairToken)
            pairBrowser(pairToken = pairToken)
        }

        adapter = WebPushBrowserAdapter(
                activity,
                onItemClick = null,
                onUnpairButtonClick = {
                   unpairBrowser(it.browserId)
                }
        )

        val browsers = api.runCatching(activity.errorSnackbar) {
            listBrowsers()
        } ?: return
        updateBrowserList(browsers)
    }

    private fun updateBrowserList(browsers: List<WebPushResponse.Browser>) {
        adapter.items = browsers
        if (b.browsersView.adapter == null) {
            b.browsersView.adapter = adapter
            b.browsersView.apply {
                isNestedScrollingEnabled = false
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(SimpleDividerItemDecoration(context))
            }
        }
        adapter.notifyDataSetChanged()

        app.config.sync.webPushEnabled = browsers.isNotEmpty()

        if (browsers.isNotEmpty()) {
            b.browsersView.visibility = View.VISIBLE
            b.browsersNoData.visibility = View.GONE
        } else {
            b.browsersView.visibility = View.GONE
            b.browsersNoData.visibility = View.VISIBLE
        }
    }

    private fun pairBrowser(browserId: String? = null, pairToken: String? = null) {
        b.scanQrCode.isEnabled = false
        b.tokenAccept.isEnabled = false
        b.tokenEditText.isEnabled = false
        b.tokenEditText.clearFocus()
        launch {
            val browsers = api.runCatching(activity.errorSnackbar) {
                pairBrowser(browserId, pairToken)
            }
            b.scanQrCode.isEnabled = true
            b.tokenAccept.isEnabled = true
            b.tokenEditText.isEnabled = true
            if (browsers != null)
                updateBrowserList(browsers)
        }
    }

    private fun unpairBrowser(browserId: String) {
        launch {
            val browsers = api.runCatching(activity.errorSnackbar) {
                unpairBrowser(browserId)
            } ?: return@launch
            updateBrowserList(browsers)
        }
    }
}
