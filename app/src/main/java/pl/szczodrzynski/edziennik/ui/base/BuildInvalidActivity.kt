/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-27.
 */

package pl.szczodrzynski.edziennik.ui.base

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.mikepenz.iconics.utils.colorInt
import pl.szczodrzynski.edziennik.databinding.ActivityBuildInvalidBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.utils.Themes

class BuildInvalidActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Themes.themeInt)
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
