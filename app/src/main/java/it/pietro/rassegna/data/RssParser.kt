package it.pietro.rassegna.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RssParser {

    private val patterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss"
    )

    /** Legge sia RSS 2.0 che Atom. */
    fun parse(input: InputStream, source: Source): List<Article> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val articles = mutableListOf<Article>()
        var title: String? = null
        var link: String? = null
        var date: String? = null
        var summary: String? = null
        var tags = mutableListOf<String>()
        var content: String? = null
        var inItem = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.lowercase(Locale.ROOT)
            when (event) {
                XmlPullParser.START_TAG -> when (tag) {
                    "item", "entry" -> {
                        inItem = true
                        title = null; link = null; date = null; summary = null; content = null
                        tags = mutableListOf()
                    }
                    "title" -> if (inItem && title == null) title = safeText(parser)
                    "link" -> if (inItem) {
                        val href = parser.getAttributeValue(null, "href")
                        val rel = parser.getAttributeValue(null, "rel")
                        if (href != null) {
                            if (link == null && (rel == null || rel == "alternate")) link = href
                        } else {
                            val text = safeText(parser)
                            if (link == null && text.isNotBlank()) link = text
                        }
                    }
                    "guid", "id" -> if (inItem && link == null) {
                        val v = safeText(parser)
                        if (v.startsWith("http")) link = v
                    }
                    "pubdate", "published", "updated", "dc:date" ->
                        if (inItem && date == null) date = safeText(parser)
                    "description", "summary" ->
                        if (inItem && summary.isNullOrBlank()) summary = safeText(parser)
                    "content:encoded", "content" -> if (inItem && content == null) {
                        val text = safeText(parser)
                        if (text.length > 400) content = text
                    }
                    "category" -> if (inItem) {
                        val term = parser.getAttributeValue(null, "term")
                        val value = if (term != null) term else safeText(parser)
                        if (value.isNotBlank()) tags.add(value)
                    }
                }
                XmlPullParser.END_TAG -> if (tag == "item" || tag == "entry") {
                    inItem = false
                    val t = title
                    val l = link
                    if (!t.isNullOrBlank() && !l.isNullOrBlank()) {
                        val cleanTitle = strip(t)
                        val cleanSummary = strip(summary ?: "").take(240)
                        articles.add(
                            Article(
                                title = cleanTitle,
                                link = l.trim(),
                                sourceName = source.name,
                                publishedAt = parseDate(date),
                                summary = cleanSummary,
                                lang = source.lang,
                                topic = Classifier.classify(source, cleanTitle, cleanSummary, tags),
                                content = content
                            )
                        )
                    }
                }
            }
            event = parser.next()
        }
        return articles
    }

    private fun safeText(parser: XmlPullParser): String = try {
        parser.nextText().trim()
    } catch (e: Exception) {
        ""
    }

    private fun parseDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        for (p in patterns) {
            try {
                val d: Date? = SimpleDateFormat(p, Locale.US).parse(raw.trim())
                if (d != null) return d.time
            } catch (e: Exception) {
                // provo il formato successivo
            }
        }
        return 0L
    }

    /** Toglie i tag HTML e normalizza le entita' piu' comuni. */
    private fun strip(input: String): String = input
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
}
