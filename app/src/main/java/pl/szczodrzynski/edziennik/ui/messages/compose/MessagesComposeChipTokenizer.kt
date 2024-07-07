/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-4.
 */

package pl.szczodrzynski.edziennik.ui.messages.compose

import androidx.appcompat.app.AppCompatActivity
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.chip.ChipSpan
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer
import pl.szczodrzynski.edziennik.data.db.entity.Teacher

class MessagesComposeChipTokenizer(
    activity: AppCompatActivity,
    nacho: NachoTextView,
    teacherList: List<Teacher>,
) : SpanChipTokenizer<ChipSpan>(
    activity,
    MessagesComposeChipCreator(
        activity = activity,
        nacho = nacho,
        teacherList = teacherList
    ),
    ChipSpan::class.java
)
