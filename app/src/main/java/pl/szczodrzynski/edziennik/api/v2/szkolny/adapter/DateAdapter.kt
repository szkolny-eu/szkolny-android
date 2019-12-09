/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.api.v2.szkolny.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import pl.szczodrzynski.edziennik.utils.models.Date

class DateAdapter : TypeAdapter<Date>() {
    override fun write(writer: JsonWriter?, value: Date?) {}

    override fun read(reader: JsonReader?): Date? {
        if (reader?.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return reader?.nextInt()?.let { Date.fromValue(it) }
    }
}
