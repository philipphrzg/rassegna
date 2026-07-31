package it.pietro.rassegna.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.pietro.rassegna.data.Article
import it.pietro.rassegna.data.Catalog
import it.pietro.rassegna.data.Classifier
import it.pietro.rassegna.data.NewsRepository
import it.pietro.rassegna.data.Prefs
import it.pietro.rassegna.data.Reader
import it.pietro.rassegna.data.ReaderResult
import it.pietro.rassegna.data.Source
import it.pietro.rassegna.data.Topic
import it.pietro.rassegna.data.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE_SIZE = 20

data class UiState(
    val ready: Boolean = false,
    val onboarded: Boolean = false,
    val loading: Boolean = false,
    val translating: Boolean = false,
    val translate: Boolean = true,
    val selected: Set<String> = emptySet(),
    val customSources: List<Source> = emptyList(),
    val articles: List<Article> = emptyList(),
    val failedSources: List<String> = emptyList(),
    val notice: String? = null,
    /** null significa "Tutte". */
    val topic: Topic? = null,
    val savedArticles: List<Article> = emptyList(),
    val showSaved: Boolean = false,
    val savingLinks: Set<String> = emptySet(),
    val fontStep: Int = 0,
    /** Quanti articoli mostrare della sezione corrente: cresce con "Carica altre notizie". */
    val pageSize: Int = PAGE_SIZE,
    /** Se non e' null, l'utente sta leggendo un articolo. */
    val reader: ReaderUi? = null
) {
    val allSources: List<Source> get() = Catalog.sources + customSources
    val activeSources: List<Source> get() = allSources.filter { it.id in selected }
    val hasForeignSources: Boolean get() = activeSources.any { it.lang != "it" }

    /** Le sezioni che oggi hanno almeno un articolo, nell'ordine dell'enum. */
    val availableTopics: List<Topic>
        get() {
            val present = articles.map { it.topic }.toSet()
            return Topic.values().filter { it in present }
        }

    /** Tutti gli articoli della sezione corrente, senza limite. */
    val visibleArticles: List<Article>
        get() {
            if (showSaved) return savedArticles
            return if (topic == null) articles else articles.filter { it.topic == topic }
        }

    /** Solo quelli mostrati finora: e' su questi che lavora la traduzione. */
    val displayedArticles: List<Article> get() = visibleArticles.take(pageSize)

    val hasMore: Boolean get() = visibleArticles.size > pageSize

    val savedLinks: Set<String> get() = savedArticles.map { it.link }.toSet()
}

private data class Snapshot(
    val onboarded: Boolean,
    val selected: Set<String>,
    val custom: List<Source>,
    val translate: Boolean
)

class NewsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val reader = Reader(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var firstLoadDone = false
    private var translationJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                prefs.onboarded,
                prefs.selectedIds,
                prefs.customSources,
                prefs.translate
            ) { onboarded, selected, custom, translate ->
                Snapshot(onboarded, selected, custom, translate)
            }.collect { snap ->
                val onboarded = snap.onboarded
                _state.value = _state.value.copy(
                    ready = true,
                    onboarded = onboarded,
                    selected = if (!onboarded && snap.selected.isEmpty()) Catalog.defaultSelection else snap.selected,
                    customSources = snap.custom,
                    translate = snap.translate
                )
                if (onboarded && !firstLoadDone) {
                    firstLoadDone = true
                    refresh()
                }
            }
        }

        viewModelScope.launch {
            combine(prefs.savedArticles, prefs.fontStep) { saved, font -> saved to font }
                .collect { (saved, font) ->
                    _state.value = _state.value.copy(savedArticles = saved, fontStep = font)
                }
        }
    }

    /* ------------------------------------------------------------ lettura */

    fun openArticle(article: Article) {
        _state.value = _state.value.copy(
            reader = ReaderUi(
                article = article,
                saved = article.link in _state.value.savedLinks
            )
        )
        loadArticle(article)
    }

    private fun loadArticle(article: Article) {
        viewModelScope.launch {
            when (val result = reader.read(article)) {
                is ReaderResult.Ok -> update {
                    it.copy(html = result.html, offline = result.offline, loading = false, error = null)
                }
                is ReaderResult.Error -> update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun retryArticle() {
        val article = _state.value.reader?.article ?: return
        update { it.copy(loading = true, error = null) }
        loadArticle(article)
    }

    fun closeReader() {
        _state.value = _state.value.copy(reader = null)
    }

    /** Salva o toglie dai salvati l'articolo aperto. */
    fun toggleSaveCurrent() {
        val current = _state.value.reader ?: return
        viewModelScope.launch {
            if (current.saved) {
                reader.forget(current.article.link)
                prefs.removeSaved(current.article.link)
                update { it.copy(saved = false, offline = false) }
            } else {
                val ok = reader.keep(current.article)
                if (ok) {
                    prefs.addSaved(current.article)
                    update { it.copy(saved = true, offline = true) }
                } else {
                    update { it.copy(error = "Non sono riuscito a salvare l'articolo.") }
                }
            }
        }
    }

    /** Scarica un articolo dalla lista, senza aprirlo. Mostra uno stato di caricamento sul segnalibro. */
    fun toggleSaveFromList(article: Article) {
        if (article.link in _state.value.savingLinks) return
        viewModelScope.launch {
            if (article.link in _state.value.savedLinks) {
                reader.forget(article.link)
                prefs.removeSaved(article.link)
                return@launch
            }
            _state.value = _state.value.copy(savingLinks = _state.value.savingLinks + article.link)
            val ok = reader.keep(article)
            _state.value = _state.value.copy(savingLinks = _state.value.savingLinks - article.link)
            if (ok) {
                prefs.addSaved(article)
            } else {
                _state.value = _state.value.copy(
                    notice = "Non sono riuscito a scaricare \"" + article.title.take(48) + "\"."
                )
            }
        }
    }

    fun translateCurrent() {
        val current = _state.value.reader ?: return
        val html = current.html ?: return
        if (current.translated) return
        viewModelScope.launch {
            update { it.copy(translating = true) }
            val error = Translator.prepare(current.article.lang)
            if (error != null) {
                update { it.copy(translating = false, error = error) }
                return@launch
            }
            val translated = reader.translateHtml(html, current.article.lang)
            update { it.copy(html = translated, translated = true, translating = false) }
        }
    }

    fun setFontStep(value: Int) {
        viewModelScope.launch { prefs.setFontStep(value) }
    }

    fun showSaved(show: Boolean) {
        _state.value = _state.value.copy(showSaved = show, topic = null, pageSize = PAGE_SIZE)
    }

    private fun update(change: (ReaderUi) -> ReaderUi) {
        val current = _state.value.reader ?: return
        _state.value = _state.value.copy(reader = change(current))
    }

    /* ------------------------------------------------------------- sezioni */

    fun setTopic(topic: Topic?) {
        _state.value = _state.value.copy(topic = topic, pageSize = PAGE_SIZE)
        if (_state.value.translate) translateVisible()
    }

    /** Chiamata dal pulsante "Carica altre notizie". */
    fun loadMore() {
        _state.value = _state.value.copy(pageSize = _state.value.pageSize + PAGE_SIZE)
        if (_state.value.translate) translateVisible()
    }

    fun toggle(id: String) {
        val current = _state.value.selected
        val next = if (id in current) current - id else current + id
        _state.value = _state.value.copy(selected = next)
        viewModelScope.launch { prefs.setSelected(next) }
    }

    fun setTranslate(value: Boolean) {
        viewModelScope.launch {
            prefs.setTranslate(value)
            if (value) {
                translateVisible()
            } else {
                translationJob?.cancel()
                _state.value = _state.value.copy(
                    translating = false,
                    notice = null,
                    articles = _state.value.articles.map { it.copy(titleIt = null, summaryIt = null) }
                )
            }
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            prefs.setSelected(_state.value.selected)
            prefs.setOnboarded()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            translationJob?.cancel()
            _state.value = _state.value.copy(loading = true, notice = null)
            val result = NewsRepository.load(_state.value.activeSources)
            val current = _state.value.topic
            val stillThere = result.articles.any { it.topic == current }
            _state.value = _state.value.copy(
                loading = false,
                articles = result.articles,
                failedSources = result.failedSources,
                topic = if (stillThere) current else null,
                pageSize = PAGE_SIZE
            )
            if (_state.value.translate) translateVisible()
        }
    }

    /**
     * Traduce titoli e sommari, ma solo degli articoli che sono davvero a schermo
     * (la pagina corrente): cosi' non si blocca a tradurne centinaia insieme.
     * Quando l'utente scorre a "Carica altre notizie", viene richiamata sulla
     * fetta nuova.
     */
    private fun translateVisible() {
        val pending = _state.value.displayedArticles.filter {
            it.titleIt == null && Translator.isSupported(it.lang)
        }
        if (pending.isEmpty()) return

        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            _state.value = _state.value.copy(translating = true)

            val languages = pending.map { it.lang }.distinct()
            for (lang in languages) {
                val error = withContext(Dispatchers.IO) { Translator.prepare(lang) }
                if (error != null) {
                    _state.value = _state.value.copy(translating = false, notice = error)
                    return@launch
                }
            }

            pending.chunked(10).forEach { chunk ->
                val done = withContext(Dispatchers.Default) {
                    chunk.map { article ->
                        article.link to Pair(
                            Translator.translate(article.title, article.lang),
