package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

object HtmlText {

    data class Ymd(val year: Int?, val month: Int, val day: Int)
    data class Hms(val hour: Int, val minute: Int, val second: Int = 0)

    private val monthForms: Map<String, Int> = buildMap {
        val genitive = listOf(
            "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
            "lipca", "sierpnia", "września", "października", "listopada", "grudnia",
        )
        val nominative = listOf(
            "styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec",
            "lipiec", "sierpień", "wrzesień", "październik", "listopad", "grudzień",
        )
        val short = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")
        for (i in 0 until 12) {
            put(genitive[i], i + 1)
            put(nominative[i], i + 1)
            put(short[i], i + 1)
        }
    }

    private val iso = Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})")
    private val dotted = Regex("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})")
    private val named = Regex("(\\d{1,2})\\s+([\\p{L}]+)\\.?(?:\\s+(\\d{4}))?")
    private val timeRegex = Regex("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?")

    fun monthNumber(name: String): Int? = monthForms[name.lowercase().trimEnd('.')]

    fun parseDate(text: String?): Ymd? {
        if (text.isNullOrBlank()) return null
        iso.find(text)?.let {
            return Ymd(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
        }
        dotted.find(text)?.let {
            return Ymd(it.groupValues[3].toInt(), it.groupValues[2].toInt(), it.groupValues[1].toInt())
        }
        for (m in named.findAll(text)) {
            val month = monthNumber(m.groupValues[2]) ?: continue
            val year = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()
            return Ymd(year, month, m.groupValues[1].toInt())
        }
        return null
    }

    fun parseTime(text: String?): Hms? {
        if (text.isNullOrBlank()) return null
        val m = timeRegex.find(text) ?: return null
        return Hms(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toIntOrNull() ?: 0)
    }

    fun parseTimeRange(text: String?): Pair<Hms, Hms>? {
        if (text.isNullOrBlank()) return null
        val all = timeRegex.findAll(text).toList()
        if (all.size < 2) return null
        fun toHms(m: MatchResult) = Hms(m.groupValues[1].toInt(), m.groupValues[2].toInt(), 0)
        return toHms(all[0]) to toHms(all[1])
    }
}
