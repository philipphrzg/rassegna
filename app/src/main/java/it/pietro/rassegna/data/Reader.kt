package it.pietro.rassegna.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import kotlin.math.abs

sealed class ReaderResult {
    data class Ok(val html: String, val offline: Boolean) : ReaderResult()
    data class Error(val message: String) : ReaderResult()
}

/**
 * Scarica la pagina di un articolo, ne estrae il testo (porta Kotlin di Readability
 * di Mozilla, con un secondo tentativo piu' semplice se il primo fallisce) e lo
 * tiene su disco se l'utente lo salva.
 */
class Reader(context: Context) {

    private val dir = File(context.filesDir, "articoli").apply { mkdirs() }

    private val browserWarnings = listOf(
        "utilizza un altro browser", "usa un browser diverso", "browser non supportato",
        "aggiorna il tuo browser", "abilita javascript", "abilitare javascript",
        "please use a different browser", "javascript is required", "enable javascript",
        "update your browser", "browser not supported"
    )

    private fun fileFor(link: String) = File(dir, abs(link.hashCode()).toString() + ".html")

    fun isDownloaded(link: String): Boolean = fileFor(link).exists()

    /** Il testo dell'articolo: prima il disco, poi il feed, infine la rete. */
    suspend fun read(article: Article): ReaderResult = withContext(Dispatchers.IO) {
        val cached = fileFor(article.link)
        if (cached.exists()) {
            return@withContext try {
                ReaderResult.Ok(cached.readText(), true)
            } catch (e: Exception) {
                ReaderResult.Error("Il file salvato non si apre. Prova a scaricarlo di nuovo.")
            }
        }

        // molti feed (WordPress e simili) portano gia' dentro l'articolo intero
        val fromFeed = article.content
        if (!fromFeed.isNullOrBlank() && fromFeed.length > 1200) {
            return@withContext ReaderResult.Ok(tidy(fromFeed, article.link), false)
        }

        val page = fetch(article.link)
            ?: return@withContext ReaderResult.Error("La pagina non risponde. Controlla la connessione.")

        val extracted = extract(page, article.link)
            ?: return@withContext ReaderResult.Error(
                "Questo sito richiede il caricamento della pagina completa: non riesco a separare l'articolo dal resto. Puoi aprirlo nel browser dal menu in alto."
            )

        ReaderResult.Ok(extracted, false)
    }

    /** Scarica e mette da parte per leggere offline. */
    suspend fun keep(article: Article): Boolean = withContext(Dispatchers.IO) {
        if (isDownloaded(article.link)) return@withContext true
        val result = read(article)
        if (result is ReaderResult.Ok) {
            try {
                fileFor(article.link).writeText(result.html)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    fun forget(link: String) {
        try {
            fileFor(link).delete()
        } catch (e: Exception) {
            // niente da fare
        }
    }

    fun clearAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Prova prima Readability (preciso quando funziona), poi un'estrazione piu'
     * semplice se il primo tentativo fallisce o restituisce un testo troppo corto
     * o un avviso del sito ("abilita JavaScript", "usa un altro browser"...).
     */
    private fun extract(html: String, url: String): String? {
        val fromReadability = try {
            val parsed = Readability4J(url, html).parse()
            parsed.contentWithUtf8Encoding ?: parsed.content
        } catch (e: Exception) {
            null
        }

        if (!fromReadability.isNullOrBlank()) {
            val cleaned = tidy(fromReadability, url)
            if (!isJunk(Jsoup.parse(cleaned).text())) return cleaned
        }

        return naiveExtract(html, url)
    }

    /** Sceglie il blocco con piu' testo, ignorando le parti sicuramente non articolo. */
    private fun naiveExtract(html: String, url: String): String? = try {
        val doc = Jsoup.parse(html, url)
        doc.select(
            "script, style, nav, header, footer, aside, form, iframe, noscript, " +
                "button, [role=navigation], .cookie, .paywall, .newsletter"
        ).remove()

        val candidates = doc.select("article, main, [class*=article], [class*=content], [class*=post], [id*=content]")
        val best: Element = candidates.maxByOrNull { it.text().length } ?: doc.body()

        if (isJunk(best.text())) null else tidy(best.outerHtml(), url)
    } catch (e: Exception) {
        null
    }

    /** Un testo troppo corto o un avviso del sito non e' un articolo leggibile. */
    private fun isJunk(text: String): Boolean {
        val clean = text.trim()
        if (clean.length < 180) return true
        val lower = clean.lowercase()
        return browserWarnings.any { lower.contains(it) }
    }

    /** Toglie script, iframe e attributi di stile, e rende le immagini adattabili. */
    private fun tidy(html: String, baseUrl: String): String = try {
        val doc = Jsoup.parse(html, baseUrl)
        doc.select("script, style, iframe, noscript, form, button, ins").remove()
        doc.select("[style]").forEach { it.removeAttr("style") }
        doc.select("[width]").forEach { it.removeAttr("width") }
        doc.select("[height]").forEach { it.removeAttr("height") }
        doc.select("img").forEach { img ->
            val real = img.attr("data-src").ifBlank { img.attr("src") }
            if (real.isBlank()) img.remove() else img.attr("src", img.absUrl("src").ifBlank { real })
        }
        doc.select("a").forEach { it.attr("href", it.absUrl("href")) }
        doc.body().html()
    } catch (e: Exception) {
        html
    }

    private fun fetch(url: String, redirects: Int = 0): String? {
        if (redirects > 3) return null
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
            }
            val code = connection.responseCode
            if (code in 301..308) {
                val location = connection.getHeaderField("Location") ?: return null
                connection.disconnect()
                return fetch(location, redirects + 1)
            }
            if (code != HttpURLConnection.HTTP_OK) return null

            val bytes = connection.inputStream.use { it.readBytes() }
            val charset = detectCharset(connection.getHeaderField("Content-Type"), bytes)
            String(bytes, charset)
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** Molti siti italiani ed europei non dichiarano UTF-8: senza questo le lettere accentate si rompono. */
    private fun detectCharset(contentType: String?, bytes: ByteArray): Charset {
        val fromHeader = contentType?.let {
            Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1)
        }
        val fromMeta = fromHeader ?: run {
            val head = String(bytes, 0, minOf(bytes.size, 2048), Charsets.ISO_8859_1)
            Regex("charset=[\"']?([\\w-]+)", RegexOption.IGNORE_CASE).find(head)?.groupValues?.get(1)
        }
        return try {
            if (fromMeta != null) Charset.forName(fromMeta) else Charsets.UTF_8
        } catch (e: Exception) {
            Charsets.UTF_8
        }
    }

    /** Traduce i paragrafi di un articolo tenendo intatta l'impaginazione. */
    suspend fun translateHtml(html: String, lang: String): String = withContext(Dispatchers.Default) {
        try {
            val doc = Jsoup.parseBodyFragment(html)
            val blocks = doc.select("p, h1, h2, h3, h4, li, blockquote, figcaption")
            for (block in blocks) {
                val original = block.text()
                if (original.length < 3) continue
                val translated = Translator.translate(original, lang)
                if (translated != original) block.text(translated)
            }
            doc.body().html()
        } catch (e: Exception) {
            html
        }
    }
}
