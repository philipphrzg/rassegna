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

    private const val AGENT = "Rassegna/1.0 (Android)"

    suspend fun load(sources: List<Source>): FeedResult = coroutineScope {
        if (sources.isEmpty()) return@coroutineScope FeedResult(emptyList(), emptyList())

        val results = sources.map { source ->
            async(Dispatchers.IO) { source to fetch(source) }
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
                connectTimeout = 12000
                readTimeout = 12000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", AGENT)
                setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
            }
            val code = connection.responseCode
            if (code in 301..308) {
                val location = connection.getHeaderField("Location") ?: return null
                connection.disconnect()
                return fetch(source.copy(url = location), redirects + 1)
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
