package it.pietro.rassegna.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class FeedResult(
    val articles: List<Article>,
    val failedSources: List<String>
)

object NewsRepository {

    /**
     * Molti siti dietro un servizio di protezione rifiutano le richieste che non
     * arrivano da un browser: con un'identificazione generica rispondevano 403 e
     * la fonte risultava "non raggiungibile" pur essendo perfettamente viva.
     */
    private const val AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"

    suspend fun load(sources: List<Source>): FeedResult = coroutineScope {
        if (sources.isEmpty()) return@coroutineScope FeedResult(emptyList(), emptyList())

        val results = sources.map { source ->
            async(Dispatchers.IO) {
                // un secondo tentativo copre i cali momentanei di un server
                source to (fetch(source) ?: fetch(source))
            }
        }.awaitAll()

        val articles = results.flatMap { it.second ?: emptyList() }
            .distinctBy { it.link }
            .sortedByDescending { it.publishedAt }

        val failed = results.filter { it.second == null }.map { it.first.name }

        FeedResult(articles, failed)
    }

    /** Scarica e legge un feed. Restituisce null se la fonte non risponde. */
    private fun fetch(source: Source, redirects: Int = 0): List<Article>? {
        if (redirects > 3) return null
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(source.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", AGENT)
                setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
                setRequestProperty("Accept-Language", "it-IT,it;q=0.9,en;q=0.8")
            }
            val code = connection.responseCode
            if (code in 301..308) {
                val location = connection.getHeaderField("Location") ?: return null
                connection.disconnect()
                // alcuni server rispondono con un indirizzo relativo
                val assoluto = URL(URL(source.url), location).toString()
                return fetch(source.copy(url = assoluto), redirects + 1)
            }
            if (code != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.use { stream ->
                RssParser.parse(stream, source)
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** Controlla un feed aggiunto a mano: restituisce il titolo del primo articolo, o null. */
    suspend fun test(url: String): String? = withContext(Dispatchers.IO) {
        val probe = Source("probe", "probe", url, "probe")
        fetch(probe)?.firstOrNull()?.title
    }
}
