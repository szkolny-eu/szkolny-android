/*
 * Copyright 2014 Mike Penz
 * Copyright 2015 Haruki Hasegawa
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
import java.util.LinkedList

@Suppress("EnumEntryName")
object SzkolnyFont : ITypeface {

    override val fontRes: Int
        get() = R.font.szkolny_font_font_v1_0

    override val characters: Map<String, Char> by lazy {
        mutableMapOf<String, Char>().apply {
            SzkolnyFont.Icon.values().associateTo(this) { it.name to it.character }
            //Icon2.values().associateTo(this) { it.name to it.character }
        }
    }

    override val mappingPrefix: String
        get() = "szf"

    override val fontName: String
        get() = "Szkolny Font"

    override val version: String
        get() = "1.0"

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

    override fun getIcon(key: String): IIcon {
        return SzkolnyFont.Icon.valueOf(key)
    }

    enum class Icon constructor(override val character: Char) : IIcon {
        szf_eye_check('\ue800'),
        szf_calendar_off('\ue801'),
        szf_file_document_edit('\ue802'),
        szf_message_off('\ue803'),
        szf_numeric_0_box_multiple_outline_off('\ue804');

        override val typeface: ITypeface by lazy { SzkolnyFont }
    }
}
