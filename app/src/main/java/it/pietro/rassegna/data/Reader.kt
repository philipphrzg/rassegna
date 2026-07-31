package it.pietro.rassegna.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

sealed class ReaderResult {
    data class Ok(val html: String, val offline: Boolean) : ReaderResult()
    data class Error(val message: String) : ReaderResult()
}

/**
 * Scarica la pagina di un articolo, ne estrae il testo (porta Kotlin di Readability
 * di Mozilla) e lo tiene su disco se l'utente lo salva.
 */
class Reader(context: Context) {

    private val dir = File(context.filesDir, "articoli").apply { mkdirs() }

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
                "Non riesco a estrarre il testo da questo sito. Puoi aprirlo nel browser dal menu in alto."
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

    private fun extract(html: String, url: String): String? = try {
        val parsed = Readability4J(url, html).parse()
        val content = parsed.contentWithUtf8Encoding ?: parsed.content
        if (content.isNullOrBlank()) null else tidy(content, url)
    } catch (e: Exception) {
        null
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
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
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
