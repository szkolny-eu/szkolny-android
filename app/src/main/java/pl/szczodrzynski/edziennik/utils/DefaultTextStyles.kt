/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-11.
 */

package pl.szczodrzynski.edziennik.utils

import android.text.style.StrikethroughSpan
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.StyledTextButtonsBinding
import pl.szczodrzynski.edziennik.core.manager.TextStylingManager.StylingConfig
import pl.szczodrzynski.edziennik.utils.span.*

object DefaultTextStyles {

    fun getAsList(b: StyledTextButtonsBinding) = listOf(
        StylingConfig.Style(
            button = b.bold,
            spanClass = BoldSpan::class.java,
            icon = CommunityMaterial.Icon2.cmd_format_bold,
            hint = R.string.hint_style_bold,
        ),
        StylingConfig.Style(
            button = b.italic,
            spanClass = ItalicSpan::class.java,
            icon = CommunityMaterial.Icon2.cmd_format_italic,
            hint = R.string.hint_style_italic,
        ),
        StylingConfig.Style(
            button = b.underline,
            // a custom span is used to prevent issues with keyboards which underline words
            spanClass = UnderlineCustomSpan::class.java,
            icon = CommunityMaterial.Icon2.cmd_format_underline,
            hint = R.string.hint_style_underline,
        ),
        StylingConfig.Style(
            button = b.strike,
            spanClass = StrikethroughSpan::class.java,
            icon = CommunityMaterial.Icon2.cmd_format_strikethrough,
            hint = R.string.hint_style_strike,
        ),
        StylingConfig.Style(
            button = b.subscript,
            spanClass = SubscriptSizeSpan::class.java,
            icon = CommunityMaterial.Icon2.cmd_format_subscript,
            hint = R.string.hint_style_subscript,
        ),
        StylingConfig.Style(
            button = b.superscript,
            spanClass = SuperscriptSizeSpan::class.java,
            icon = CommunityMaterial.Icon2.cmd_format_superscript,
            hint = R.string.hint_style_superscript,
        ),
    )
}
