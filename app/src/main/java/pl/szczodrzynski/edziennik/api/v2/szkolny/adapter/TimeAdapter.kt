/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.api.v2.szkolny.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import pl.szczodrzynski.edziennik.utils.models.Time

class TimeAdapter : TypeAdapter<Time>() {
    override fun write(writer: JsonWriter?, time: Time?) {
        if (time == null) {
            writer?.nullValue()
        } else {
            writer?.value(time.value)
        }
    }

    override fun read(reader: JsonReader?): Time? {
        if (reader?.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return reader?.nextInt()?.let { Time.fromValue(it) }
    }
}
