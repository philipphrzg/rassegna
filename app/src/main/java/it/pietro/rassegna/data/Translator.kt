package it.pietro.rassegna.data

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.mlkit.nl.translate.Translator as MlTranslator

/**
 * Traduzione in italiano sul telefono, con ML Kit.
 * Il modello di ogni lingua si scarica una volta sola (circa 30 MB) e poi
 * funziona anche senza rete.
 */
object Translator {

    private val clients = mutableMapOf<String, MlTranslator>()
    private val ready = mutableSetOf<String>()
    private val cache = Collections.synchronizedMap(LinkedHashMap<String, String>())
    private const val CACHE_LIMIT = 1500

    /** Lingue che sappiamo tradurre verso l'italiano. */
    fun isSupported(lang: String): Boolean =
        lang != "it" && TranslateLanguage.fromLanguageTag(lang) != null

    /**
     * Prepara il modello della lingua. Restituisce null se e' pronto,
     * altrimenti il messaggio d'errore da mostrare.
     */
    suspend fun prepare(lang: String): String? {
        if (lang in ready) return null
        val client = client(lang) ?: return "Lingua non supportata: " + lang
        return try {
            client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            ready.add(lang)
            null
        } catch (e: Exception) {
            "Non sono riuscito a scaricare il dizionario " + lang.uppercase() + ". Controlla la connessione."
        }
    }

    /** Traduce un testo. Se qualcosa va storto restituisce l'originale. */
    suspend fun translate(text: String, lang: String): String {
        if (text.isBlank() || !isSupported(lang)) return text
        val key = lang + "|" + text
        cache[key]?.let { return it }

        val client = client(lang) ?: return text
        if (lang !in ready && prepare(lang) != null) return text

        return try {
            val result = client.translate(text).await()
            if (cache.size > CACHE_LIMIT) cache.clear()
            cache[key] = result
            result
        } catch (e: Exception) {
            text
        }
    }

    private fun client(lang: String): MlTranslator? {
        clients[lang]?.let { return it }
        val source = TranslateLanguage.fromLanguageTag(lang) ?: return null
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(TranslateLanguage.ITALIAN)
            .build()
        val client = Translation.getClient(options)
        clients[lang] = client
        return client
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { value -> if (cont.isActive) cont.resume(value) }
        addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
    }
}
