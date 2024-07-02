/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-27.
 */

package pl.szczodrzynski.edziennik.ui.main

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.mikepenz.iconics.utils.colorInt
import pl.szczodrzynski.edziennik.databinding.ActivityBuildInvalidBinding
import pl.szczodrzynski.edziennik.ext.app
import pl.szczodrzynski.edziennik.ext.onClick

class BuildInvalidActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.uiManager.applyTheme(this)
        val b = ActivityBuildInvalidBinding.inflate(layoutInflater, null, false)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)

        b.icon.icon?.colorInt = intent.getIntExtra("color", Color.GREEN)
        b.message.text = intent.getStringExtra("message")
        b.closeButton.isVisible = !intent.getBooleanExtra("isCritical", true)
        b.closeButton.onClick {
            finish()
        }
    }
}
