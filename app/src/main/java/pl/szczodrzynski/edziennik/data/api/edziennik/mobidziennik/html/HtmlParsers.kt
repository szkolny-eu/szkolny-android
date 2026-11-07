package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import java.io.StringReader

object HtmlParsers {

    private val whitespace = Regex("\\s+")
    private val timetableLeft = Regex("left:\\s*([\\d.]+)%")
    private val hoursRegex = Regex("^\\d\\d:\\d\\d\\s*-\\s*\\d\\d:\\d\\d")
    private val roomSuffix = Regex("\\(\\w+\\)$")
    private val brRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val eventsKey = Regex("\\bevents\\s*:\\s*")

    fun isLoginPage(text: String): Boolean =
        text.contains("id=\"haslo\"") && text.contains("name=\"haslo\"")

    fun clean(s: String?): String =
        (s ?: "").replace('\u00a0', ' ').replace(whitespace, " ").trim()

    private fun soup(text: String): Document = Jsoup.parse(text)

    private fun content(doc: Document): Element? = doc.selectFirst("#content") ?: doc.body()

    private fun Element.kids(vararg tags: String): List<Element> =
        children().filter { it.tagName() in tags }

    private fun Element.kid(tag: String): Element? = children().firstOrNull { it.tagName() == tag }

    private fun collectTexts(node: Node, out: MutableList<String>) {
        if (node is TextNode) out += node.text()
        else node.childNodes().forEach { collectTexts(it, out) }
    }

    fun getText(el: Element?, sep: String = ""): String {
        if (el == null) return ""
        val out = mutableListOf<String>()
        collectTexts(el, out)
        return out.joinToString(sep)
    }

    fun infoFields(span: Element?): Map<String, String> {
        val out = linkedMapOf<String, String>()
        if (span == null) return out
        for (strong in span.select("strong")) {
            val labelNode = strong.previousSibling()
            val label = clean((labelNode as? TextNode)?.text() ?: "").trimEnd(':').trim()
            if (label.isNotEmpty())
                out[label] = clean(getText(strong, " "))
        }
        return out
    }

    private fun tableRows(table: Element): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        for (tr in table.select("tr")) {
            val cells = tr.kids("td", "th").map { clean(getText(it, " ")) }
            if (cells.any { it.isNotEmpty() })
                rows += cells
        }
        return rows
    }

    private fun parseFloat(s: String?): Double? = s?.trim()?.replace(",", ".")?.toDoubleOrNull()

    data class Grade(
        val id: String,
        val ocena: String?,
        val wartosc: Double?,
        val waga: Double?,
        val kategoria: String?,
        val opis: String,
        val liczonaDoSredniej: Boolean,
        val wpisano: String?,
        val nauczyciel: String?,
    )

    data class SubjectGrades(val przedmiot: String, val okres: Int, val oceny: List<Grade>)

    fun parseGrades(text: String, okres: Int): List<SubjectGrades> {
        val doc = content(soup(text)) ?: return emptyList()
        val table = doc.selectFirst("table.spis") ?: return emptyList()
        val subjects = mutableListOf<SubjectGrades>()
        for (tr in table.select("tr.naglowek3")) {
            val tds = tr.kids("td")
            if (tds.size < 2) continue
            val nameDiv = tds[0].selectFirst("div") ?: continue
            nameDiv.select("small").remove()
            val subject = clean(getText(nameDiv))
            val grades = mutableListOf<Grade>()
            var category: String? = null
            for (el in tds[1].childNodes()) {
                if (el !is Element) continue
                if (el.tagName() == "span") {
                    category = clean(getText(el)).trimEnd(':').trim()
                } else if (el.tagName() == "a" && el.hasClass("info")) {
                    val strong = el.kid("strong")
                    val sup = el.kid("sup")
                    val small = el.kid("small")
                    val f = infoFields(el.kid("span"))
                    val value = parseFloat(f["Wartość oceny"])
                    val weight = parseFloat(clean(getText(sup)))
                    grades += Grade(
                        id = el.attr("rel").split(" ").filter { it.isNotEmpty() }.joinToString(" "),
                        ocena = strong?.let { clean(getText(it)) },
                        wartosc = value,
                        waga = weight,
                        kategoria = category,
                        opis = clean(getText(small)).trim { it == '(' || it == ')' || it == ' ' },
                        liczonaDoSredniej = (f["Liczona do średniej"] ?: "").lowercase().startsWith("tak"),
                        wpisano = f["Wpisano"],
                        nauczyciel = f["Wpisał(a)"],
                    )
                }
            }
            subjects += SubjectGrades(subject, okres, grades)
        }
        return subjects
    }

    data class Test(
        val data: String,
        val dzienGodzina: String,
        val klasa: String,
        val przedmiot: String,
        val rodzaj: String,
        val tematyka: String,
        val nauczyciel: String?,
        val wpisano: String?,
        val zmianaTerminu: String?,
    )

    fun parseTests(text: String): List<Test> {
        val doc = content(soup(text)) ?: return emptyList()
        val out = mutableListOf<Test>()
        for (tr in doc.select("table.spis tr")) {
            val tds = tr.kids("td")
            if (tds.size != 6) continue
            val whenEl = tds[0].selectFirst("small")
            val span = tds[1].selectFirst("span")
            val przedmiot = tds[2].selectFirst("span")
            val rodzaj = tds[3].selectFirst("span")
            val f = infoFields(tds[5].selectFirst("span"))
            var dateTxt = clean(getText(tds[0], " "))
            if (whenEl != null)
                dateTxt = clean(dateTxt.replace(clean(getText(whenEl)), ""))
            out += Test(
                data = dateTxt,
                dzienGodzina = if (whenEl != null) clean(getText(whenEl)) else "",
                klasa = span?.attr("title") ?: clean(getText(tds[1])),
                przedmiot = przedmiot?.attr("title") ?: clean(getText(tds[2])),
                rodzaj = rodzaj?.attr("title") ?: clean(getText(tds[3])),
                tematyka = clean(getText(tds[4], " ")),
                nauczyciel = f["Wpisał(a)"],
                wpisano = f["Czas wpisania"],
                zmianaTerminu = f["Ostatnia zmiana terminu"],
            )
        }
        return out
    }

    data class Homework(
        val id: String,
        val uczen: String?,
        val termin: String,
        val godziny: String,
        val przedmiot: String,
        val nauczyciel: String?,
        val wpisano: String?,
        val tresc: String,
        val trescUcieta: Boolean,
        val odpowiedzWyslana: String,
        val zrobione: Boolean?,
    )

    fun parseHomework(text: String): List<Homework> {
        val doc = content(soup(text)) ?: return emptyList()
        val out = mutableListOf<Homework>()
        for (tr in doc.select("tr[id_zadania]")) {
            val tds = tr.kids("td")
            if (tds.size < 4) continue
            val whenEl = tds[0].selectFirst("small")
            var dateTxt = clean(getText(tds[0], " "))
            if (whenEl != null)
                dateTxt = clean(dateTxt.replace(clean(getText(whenEl)), ""))
            val f = infoFields(tds[2].selectFirst("span"))
            val trescTd = tds[3]
            val tresc = trescTd.attr("title").ifEmpty { clean(getText(trescTd, " ")) }
            val done = if (tds.size > 5) tds[5].selectFirst("img") else null
            out += Homework(
                id = tr.attr("id_zadania"),
                uczen = tr.attr("uczen").ifEmpty { null },
                termin = dateTxt,
                godziny = if (whenEl != null) clean(getText(whenEl)) else "",
                przedmiot = clean(getText(tds[1])),
                nauczyciel = f["Wpisał(a)"],
                wpisano = f["Czas wpisania"],
                tresc = clean(tresc),
                trescUcieta = clean(tresc).endsWith("..."),
                odpowiedzWyslana = if (tds.size > 4) clean(getText(tds[4])) else "",
                zrobione = done?.let { it.attr("alt") == "tak" },
            )
        }
        return out
    }

    fun parseHomeworkDetails(text: String, zid: String): Map<String, String> {
        val doc = content(soup(text)) ?: return mapOf("id" to zid)
        doc.select("form, script, style, input, textarea, select, button, a.button, a.button_right").remove()
        doc.select("a.info > span, th span").remove()
        val fields = linkedMapOf<String, String>()
        for (t in doc.select("table")) {
            for (r in tableRows(t)) {
                if (r.size == 2)
                    fields[r[0].split(":")[0].trim()] = r[1]
            }
        }
        if (fields.isNotEmpty())
            return linkedMapOf("id" to zid) + fields
        val plain = getText(doc, "\n").split("\n").map { clean(it) }.filter { it.isNotEmpty() }
        return mapOf("id" to zid, "tekst" to plain.joinToString("\n"))
    }

    data class MessageRow(
        val id: String,
        val temat: String,
        val zalaczniki: String,
        val nadawca: String,
        val otrzymano: String,
        val przeczytana: Boolean,
    )

    fun parseMessageList(text: String): List<MessageRow> {
        val doc = content(soup(text)) ?: return emptyList()
        val out = mutableListOf<MessageRow>()
        for (tr in doc.select("table.spis tr[rel]")) {
            val tds = tr.kids("td")
            if (tds.size < 6) continue
            out += MessageRow(
                id = tr.attr("rel"),
                temat = clean(getText(tds[0], " ")),
                zalaczniki = clean(getText(tds[1])),
                nadawca = clean(getText(tds[3], " ")),
                otrzymano = clean(getText(tds[4], " ")),
                przeczytana = clean(getText(tds[5])).lowercase().startsWith("tak"),
            )
        }
        return out
    }

    data class Message(
        val id: String,
        val temat: String,
        val nadawca: String?,
        val wyslano: String?,
        val przeczytano: String?,
        val tresc: String,
        val zalaczniki: List<String>,
    )

    fun parseMessage(text: String, mid: String): Message {
        val doc = content(soup(text)) ?: return Message(mid, "", null, null, null, "", emptyList())
        val h1 = doc.selectFirst("h1")
        val body = doc.selectFirst("div.wiadomosc_tresc")
        val meta = linkedMapOf<String, String>()
        if (body != null) {
            val metaDivs = body.kids("div")
            if (metaDivs.isNotEmpty()) {
                val m = metaDivs.last()
                for (line in getText(m, "\n").split("\n")) {
                    if (":" in line) {
                        val k = line.substringBefore(":")
                        val v = line.substringAfter(":")
                        meta[clean(k)] = clean(v)
                    }
                }
                m.remove()
            }
        }
        val attachments = mutableListOf<String>()
        for (h in doc.select("h1")) {
            if ("Załączniki" in getText(h)) {
                var sib: Node? = h.nextSibling()
                while (sib != null) {
                    if (sib is Element) {
                        if (sib.tagName() == "h1") break
                        if (sib.tagName() == "a") attachments += clean(getText(sib))
                        else attachments += sib.select("a").map { clean(getText(it)) }
                    }
                    sib = sib.nextSibling()
                }
            }
        }
        return Message(
            id = mid,
            temat = if (h1 != null) clean(getText(h1)) else "",
            nadawca = meta["nadawca"],
            wyslano = meta["czas wysłania"],
            przeczytano = meta["czas przeczytania"],
            tresc = if (body != null)
                getText(body, "\n").split("\n").map { clean(it) }.filter { it.isNotEmpty() }.joinToString("\n")
            else "",
            zalaczniki = attachments,
        )
    }

    data class CalendarEvent(
        val data: String?,
        val koniec: String?,
        val typ: String,
        val idRaw: String,
        val kolor: String?,
        val tytul: String,
        val opis: String,
    )

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    fun parseClassCalendar(text: String): List<CalendarEvent> {
        val m = eventsKey.find(text) ?: return emptyList()
        val reader = JsonReader(StringReader(text.substring(m.range.last + 1))).apply { isLenient = true }
        val events = JsonParser.parseReader(reader)
        if (!events.isJsonArray) return emptyList()
        val out = events.asJsonArray.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }.map { e ->
            val idRaw = e.str("id") ?: ""
            CalendarEvent(
                data = e.str("start"),
                koniec = e.str("end"),
                typ = idRaw.split(";")[0],
                idRaw = idRaw,
                kolor = e.str("color"),
                tytul = clean(Parser.unescapeEntities(getText(soup(e.str("title") ?: "").body()), false)),
                opis = clean(getText(soup(Parser.unescapeEntities(e.str("comment") ?: "", false)).body(), " ")),
            )
        }
        return out.sortedBy { it.data ?: "" }
    }

    data class SchoolCalendarRow(
        val termin: String,
        val nazwa: String,
        val kategoria: String,
        val pozostalo: String,
        val opis: String,
    )

    fun parseSchoolCalendar(text: String): List<SchoolCalendarRow> {
        val doc = content(soup(text)) ?: return emptyList()
        val out = mutableListOf<SchoolCalendarRow>()
        for (tr in doc.select("table.spis tr.podswietl")) {
            val tds = tr.kids("td")
            if (tds.size < 3) continue
            val nxt = tr.nextElementSibling()
            val opis = if (nxt != null && !nxt.hasClass("podswietl")) clean(getText(nxt, " ")) else ""
            out += SchoolCalendarRow(
                termin = clean(getText(tds[0], " ")),
                nazwa = clean(getText(tds[1], " ")),
                kategoria = clean(getText(tds[2], " ")),
                pozostalo = if (tds.size > 3) clean(getText(tds[3], " ")) else "",
                opis = opis,
            )
        }
        return out
    }

    data class TimetableDay(val dzien: String, val data: String?)

    data class TimetableLesson(
        val dzien: String?,
        val data: String?,
        val godziny: String,
        val przedmiot: String,
        val szczegoly: String,
        val status: String?,
        val uwaga: String,
        val rodzaj: String? = null,
    )

    data class Timetable(val dni: List<TimetableDay>, val lekcje: List<TimetableLesson>)

    fun parseTimetable(text: String): Timetable {
        val doc = content(soup(text)) ?: return Timetable(emptyList(), emptyList())
        val days = doc.select(".plansc_top > div").map {
            TimetableDay(clean(getText(it)), it.attr("title").ifEmpty { null })
        }
        val lessons = mutableListOf<TimetableLesson>()
        for (bx in doc.select("div.bx")) {
            val style = bx.attr("style")
            val idx = timetableLeft.find(style)?.groupValues?.get(1)?.toDoubleOrNull()?.let { (it / 20).toInt() }
            val raw = Parser.unescapeEntities(bx.attr("title"), false)
            var lines = raw.split(brRegex).map { clean(getText(soup(it).body())) }
            lines = lines.filter { it.isNotEmpty() }
            val hours = lines.firstOrNull { hoursRegex.containsMatchIn(it) } ?: ""
            var rest = lines.filter { it != hours }
            var uwaga = rest.filter {
                ":" in it && !roomSuffix.containsMatchIn(it) && rest.indexOf(it) == 0 &&
                    it.lowercase().let { l ->
                        l.startsWith("lekcja") || l.startsWith("zastępstwo") || l.startsWith("zmiana")
                    }
            }
            val kind = bx.classNames().firstOrNull { it == "pz" || it == "lz" || it == "lo" }
            if (uwaga.isEmpty() && (kind == "lz" || kind == "lo") && rest.size >= 3)
                uwaga = listOf(rest[0])
            rest = rest.filter { it !in uwaga }
            val day = idx?.let { days.getOrNull(it) }
            lessons += TimetableLesson(
                dzien = day?.dzien,
                data = day?.data,
                godziny = hours,
                przedmiot = rest.firstOrNull() ?: "",
                szczegoly = rest.drop(1).joinToString(" | "),
                status = bx.attr("alt").ifEmpty { null },
                uwaga = uwaga.joinToString(" "),
                rodzaj = kind,
            )
        }
        val order = days.withIndex().associate { it.value.dzien to it.index }
        return Timetable(days, lessons.sortedWith(compareBy({ order[it.dzien] ?: 99 }, { it.godziny })))
    }

    data class Substitution(
        val dzien: String?,
        val godzinyWPlanie: String,
        val lekcjaWPlanie: String,
        val zmiana: String,
        val sala: String,
        val data: String,
        val godziny: String,
    )

    fun parseSubstitutions(text: String): List<Substitution> {
        val doc = content(soup(text)) ?: return emptyList()
        val out = mutableListOf<Substitution>()
        var date: String? = null
        for (tr in doc.select("table.spis tr")) {
            val h2 = tr.selectFirst("h2")
            if (h2 != null) {
                date = clean(getText(h2))
                continue
            }
            val tds = tr.kids("td")
            if (tds.size == 6) {
                val c = tds.map { clean(getText(it, " ")) }
                out += Substitution(date, c[0], c[1], c[2], c[3], c[4], c[5])
            }
        }
        return out
    }

    data class AttendanceLesson(val godziny: String, val przedmiot: String, val nauczyciel: String, val status: String)
    data class AttendanceEntry(val data: String, val podsumowanie: String, val lekcje: List<AttendanceLesson>)

    fun parseAttendance(text: String): List<AttendanceEntry> {
        val doc = content(soup(text)) ?: return emptyList()
        val out = mutableListOf<AttendanceEntry>()
        for (tr in doc.select("table.spis tr.podswietl")) {
            val tds = tr.kids("td")
            if (tds.size < 2) continue
            val lekcje = mutableListOf<AttendanceLesson>()
            val det = tr.nextElementSibling()
            if (det != null && det.hasClass("szczegoly_nieobecnosci")) {
                for (r in det.select("table tr")) {
                    val c = r.kids("td")
                    if (c.size >= 4) {
                        val img = c[3].selectFirst("img")
                        lekcje += AttendanceLesson(
                            godziny = clean(getText(c[0])),
                            przedmiot = clean(getText(c[1])),
                            nauczyciel = clean(getText(c[2])),
                            status = if (img != null) img.attr("alt") else clean(getText(c[3])),
                        )
                    }
                }
            }
            out += AttendanceEntry(clean(getText(tds[0])), clean(getText(tds[1], " ")), lekcje)
        }
        return out
    }

    fun parseGenericTables(text: String): List<List<List<String>>> {
        val doc = content(soup(text)) ?: return emptyList()
        doc.select("th a.info > span").remove()
        val result = mutableListOf<List<List<String>>>()
        for (t in doc.select("table.spis")) {
            if (t.parents().any { it.tagName() == "table" }) continue
            result += listOf(tableRows(t))
        }
        return result
    }

    data class FinalGrade(val przedmiot: String, val naglowek: String, val ocena: String)

    fun parseFinalGrades(text: String): List<FinalGrade> {
        val out = mutableListOf<FinalGrade>()
        for (table in parseGenericTables(text)) {
            val header = table.firstOrNull() ?: continue
            val h = header.map { it.lowercase() }
            if (h.none { "roczn" in it || "śródrocz" in it || "srodrocz" in it })
                continue
            for (row in table.drop(1)) {
                val subject = row.firstOrNull()?.takeIf { it.isNotEmpty() } ?: continue
                for (j in 1 until minOf(header.size, row.size)) {
                    val cell = row[j].trim()
                    if (cell.isEmpty() || header[j].isEmpty()) continue
                    out += FinalGrade(subject, header[j], cell)
                }
            }
        }
        return out
    }

    fun parseAnnouncements(text: String): String {
        val doc = content(soup(text)) ?: return ""
        doc.select("h1").remove()
        return clean(getText(doc, " "))
    }
}
