package it.pietro.rassegna.data

data class Source(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    /** Codice lingua del feed: it, en, fr, de, es, pt. Serve alla traduzione. */
    val lang: String = "it",
    /** Se la fonte tratta un solo tema, la sezione e' gia' decisa. */
    val topic: Topic? = null,
    val custom: Boolean = false
) {
    fun encode(): String = listOf(id, name, url, category, lang).joinToString("\u0001")

    companion object {
        fun decode(raw: String): Source? {
            val p = raw.split("\u0001")
            if (p.size < 4) return null
            return Source(
                id = p[0],
                name = p[1],
                url = p[2],
                category = p[3],
                lang = p.getOrElse(4) { "it" },
                custom = true
            )
        }
    }
}

data class Article(
    val title: String,
    val link: String,
    val sourceName: String,
    val publishedAt: Long,
    val summary: String,
    val lang: String = "it",
    val topic: Topic = Topic.ALTRO,
    val titleIt: String? = null,
    val summaryIt: String? = null,
    /** Testo completo, quando il feed lo porta gia' con se'. */
    val content: String? = null
) {
    val shownTitle: String get() = titleIt ?: title
    val shownSummary: String get() = summaryIt ?: summary
    val isTranslated: Boolean get() = titleIt != null

    /** Solo i dati che servono a ritrovare un articolo salvato. */
    fun encodeSaved(): String = listOf(
        link, title, sourceName, publishedAt.toString(), lang, topic.name,
        summary, titleIt ?: "", summaryIt ?: ""
    ).joinToString("\u0001")

    companion object {
        fun decodeSaved(raw: String): Article? {
            val p = raw.split("\u0001")
            if (p.size < 6) return null
            return Article(
                title = p[1],
                link = p[0],
                sourceName = p[2],
                publishedAt = p[3].toLongOrNull() ?: 0L,
                summary = p.getOrElse(6) { "" },
                lang = p[4],
                topic = runCatching { Topic.valueOf(p[5]) }.getOrDefault(Topic.ALTRO),
                titleIt = p.getOrElse(7) { "" }.ifBlank { null },
                summaryIt = p.getOrElse(8) { "" }.ifBlank { null }
            )
        }
    }
}
