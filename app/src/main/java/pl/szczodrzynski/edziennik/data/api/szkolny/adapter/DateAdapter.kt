/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import pl.szczodrzynski.edziennik.utils.models.Date

class DateAdapter : TypeAdapter<Date>() {
    override fun write(writer: JsonWriter?, date: Date?) {
        if (date == null) {
            writer?.nullValue()
        } else {
            writer?.value(date.value)
        }
    }

    override fun read(reader: JsonReader?): Date? {
        if (reader?.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return reader?.nextInt()?.let { Date.fromValue(it) }
    }
}
