/*
 * Copyright 2019 Mike Penz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mikepenz.iconics.typeface.library.szkolny.font

import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.ITypeface
import com.mikepenz.iconics.typeface.library.szkolny.R
import java.util.*

@Suppress("EnumEntryName")
object SzkolnyFont : ITypeface {

    override val fontRes: Int
        get() = R.font.szkolny_font_font_v1_1

    override val characters: Map<String, Char> by lazy {
        Icon.values().associate { it.name to it.character }
    }
    
    override val mappingPrefix: String
        get() = "szf"

    override val fontName: String
        get() = "Szkolny Font"

    override val version: String
        get() = "1.1"

    override val iconCount: Int
        get() = characters.size

    override val icons: List<String>
        get() = characters.keys.toCollection(LinkedList())

    override val author: String
        get() = "Kuba"

    override val url: String
        get() = ""

    override val description: String
        get() = ""

    override val license: String
        get() = ""

    override val licenseUrl: String
        get() = ""

    override fun getIcon(key: String): IIcon = Icon.valueOf(key)

    enum class Icon constructor(override val character: Char) : IIcon {
        szf_alarm_bell_outline('\ue800'),
		szf_calendar_plus_outline('\ue801'),
		szf_calendar_today_outline('\ue802'),
		szf_clipboard_list_outline('\ue803'),
		szf_delete_empty_outline('\ue804'),
		szf_discord_outline('\ue805'),
		szf_file_code_outline('\ue806'),
		szf_file_excel_outline('\ue807'),
		szf_file_image_outline('\ue808'),
		szf_file_music_outline('\ue809'),
		szf_file_pdf_outline('\ue80a'),
		szf_file_percent_outline('\ue80b'),
		szf_file_powerpoint_outline('\ue80c'),
		szf_file_video_outline('\ue80d'),
		szf_file_word_outline('\ue80e'),
		szf_message_processing_outline('\ue80f'),
		szf_notebook_outline('\ue810'),
		szf_zip_box_outline('\ue811');

        override val typeface: ITypeface by lazy { SzkolnyFont }
    }
}