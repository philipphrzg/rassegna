package it.pietro.rassegna.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pietro.rassegna.data.Article
import it.pietro.rassegna.data.Source
import it.pietro.rassegna.data.Topic
import kotlinx.coroutines.launch

private val LANGUAGES = listOf(
    "it" to "Italiano",
    "en" to "Inglese",
    "fr" to "Francese",
    "de" to "Tedesco",
    "es" to "Spagnolo",
    "pt" to "Portoghese"
)

/* ---------------------------------------------------------------- Onboarding */

@Composable
fun OnboardingScreen(
    sources: List<Source>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Button(
                    onClick = onDone,
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        if (selected.isEmpty()) "Scegli almeno una fonte"
                        else "Mostrami le notizie (" + selected.size + ")"
                    )
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(Modifier.padding(20.dp, 28.dp, 20.dp, 4.dp)) {
                    Text("Che cosa vuoi leggere", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scegli le fonti che ti interessano. Le testate straniere arrivano tradotte in italiano; puoi cambiare tutto in seguito dal pulsante Fonti.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            sourceList(sources, selected, onToggle, null)
        }
    }
}

/* --------------------------------------------------------------------- Feed */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    articles: List<Article>,
    topics: List<Topic>,
    selectedTopic: Topic?,
    savedLinks: Set<String>,
    savingLinks: Set<String>,
    showSaved: Boolean,
    hasMore: Boolean,
    loading: Boolean,
    translating: Boolean,
    failed: List<String>,
    notice: String?,
    onTopicChange: (Topic?) -> Unit,
    onOpenArticle: (Article) -> Unit,
    onToggleSave: (Article) -> Unit,
    onShowSaved: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSources: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showSaved) "Salvati" else "Rassegna",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = { onShowSaved(!showSaved) }) {
                        Icon(
                            if (showSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (showSaved) "Torna alle notizie" else "Articoli salvati",
                            tint = if (showSaved) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna")
                    }
                    IconButton(onClick = onOpenSources) {
                        Icon(Icons.Filled.Tune, contentDescription = "Fonti")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            if (topics.size > 1 && !showSaved) {
                TopicBar(topics, selectedTopic, onTopicChange)
            }
            if (loading || translating) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (translating) {
                Banner("Traduco gli articoli stranieri...")
            }
            if (notice != null) {
                Banner(notice)
            }
            if (articles.isEmpty() && !loading) {
                if (showSaved) EmptySaved() else EmptySection(selectedTopic, onOpenSources)
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (failed.isNotEmpty()) {
                        item {
                            Text(
                                "Non hanno risposto: " + failed.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp, 12.dp)
                            )
                        }
                    }
                    items(articles, key = { it.link }) { article ->
                        ArticleRow(
                            article = article,
                            saved = article.link in savedLinks,
                            saving = article.link in savingLinks,
                            onClick = { onOpenArticle(article) },
                            onToggleSave = { onToggleSave(article) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (hasMore) {
                        item(key = "load-more") {
                            LoadMoreRow(onLoadMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadMoreRow(onLoadMore: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoadMore)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Carica altre notizie",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TopicBar(
    topics: List<Topic>,
    selected: Topic?,
    onChange: (Topic?) -> Unit
) {
    val entries: List<Topic?> = listOf(null) + topics
    val index = entries.indexOf(selected).coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = index,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 12.dp,
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        entries.forEachIndexed { i, topic ->
            Tab(
                selected = i == index,
                onClick = { onChange(topic) },
                text = {
                    Text(
                        (topic?.label ?: "Tutte").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Banner(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp, 10.dp)
    )
}

@Composable
private fun EmptySection(topic: Topic?, onOpenSources: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                if (topic == null) "Ancora niente da leggere" else "Niente in " + topic.label + " per ora",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (topic == null) "Attiva qualche fonte oppure aggiorna la pagina."
                else "Aggiungi una fonte che segue questo tema, oppure aggiorna la pagina.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenSources) { Text("Scegli le fonti") }
        }
    }
}

@Composable
private fun EmptySaved() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Nessun articolo salvato", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tocca il segnalibro accanto a un articolo per scaricarlo e tenerlo qui, anche senza rete.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArticleRow(
    article: Article,
    saved: Boolean,
    saving: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit
) {
    var showOriginal by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 6.dp, top = 16.dp, bottom = 16.dp)
    ) {
      Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                article.sourceName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (article.publishedAt > 0) {
                Text(
                    "  ·  " + relativeTime(article.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (article.isTranslated) {
                Text(
                    "  ·  TRADOTTO DA " + article.lang.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (showOriginal) article.title else article.shownTitle,
            style = MaterialTheme.typography.titleMedium
        )
        val body = if (showOriginal) article.summary else article.shownSummary
        if (body.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (article.isTranslated) {
            Spacer(Modifier.height(6.dp))
            Text(
                if (showOriginal) "Mostra la traduzione" else "Mostra l'originale",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showOriginal = !showOriginal }
            )
        }
      }
      if (saving) {
          Box(Modifier.width(48.dp).height(48.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(
                  Modifier.width(18.dp).height(18.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary
              )
          }
      } else {
          IconButton(onClick = onToggleSave) {
              Icon(
                  if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                  contentDescription = if (saved) "Togli dai salvati" else "Scarica e salva",
                  tint = if (saved) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant
              )
          }
      }
    }
}

/* -------------------------------------------------------------------- Fonti */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    sources: List<Source>,
    selected: Set<String>,
    translate: Boolean,
    onToggle: (String) -> Unit,
    onTranslateChange: (Boolean) -> Unit,
    onRemoveCustom: (String) -> Unit,
    onAddCustom: suspend (String, String, String) -> String?,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Fonti", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Torna alle notizie")
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Aggiungi un feed")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Traduci in italiano", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Titoli e sommari delle testate straniere. La traduzione avviene sul telefono: la prima volta scarica il dizionario della lingua (circa 30 MB), poi funziona anche offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = translate, onCheckedChange = onTranslateChange)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            sourceList(sources, selected, onToggle, onRemoveCustom)
        }
    }

    if (showDialog) {
        AddSourceDialog(
            onDismiss = { showDialog = false },
            onAdd = onAddCustom
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: suspend (String, String, String) -> String?
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("it") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi un feed") },
        text = {
            Column {
                Text(
                    "Incolla l'indirizzo RSS di un sito che segui.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = { Text("Indirizzo del feed") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome (facoltativo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Lingua del feed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LANGUAGES.take(4).forEach { (code, label) ->
                        FilterChip(
                            selected = lang == code,
                            onClick = { lang = code },
                            label = { Text(code.uppercase()) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank() && !checking,
                onClick = {
                    checking = true
                    scope.launch {
                        val result = onAdd(name, url, lang)
                        checking = false
                        if (result == null) onDismiss() else error = result
                    }
                }
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        Modifier
                            .width(18.dp)
                            .height(18.dp)
                    )
                } else {
                    Text("Verifica e aggiungi")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

/* ------------------------------------------------------------------ Comune */

private fun LazyListScope.sourceList(
    sources: List<Source>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onRemoveCustom: ((String) -> Unit)?
) {
    val grouped = sources.groupBy { it.category }
    grouped.forEach { (category, list) ->
        item(key = "header-" + category) {
            Text(
                category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)
            )
        }
        items(list, key = { it.id }) { source ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(source.id) }
                    .padding(start = 12.dp, end = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = source.id in selected,
                        onCheckedChange = { onToggle(source.id) }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(source.name, style = MaterialTheme.typography.bodyMedium)
                        if (source.lang != "it") {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                source.lang.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (source.custom && onRemoveCustom != null) {
                    IconButton(onClick = { onRemoveCustom(source.id) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Rimuovi " + source.name,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun relativeTime(time: Long): String = DateUtils.getRelativeTimeSpanString(
    time,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS
).toString()
