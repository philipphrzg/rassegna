package it.pietro.rassegna.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.pietro.rassegna.data.Article

data class ReaderUi(
    val article: Article,
    val html: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val saved: Boolean = false,
    val offline: Boolean = false,
    val translating: Boolean = false,
    val translated: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    ui: ReaderUi,
    fontStep: Int,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onTranslate: () -> Unit,
    onFontStep: (Int) -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val canTranslate = ui.article.lang != "it"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        ui.article.sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Torna alla lista")
                    }
                },
                actions = {
                    TextButton(onClick = { onFontStep(fontStep - 1) }, enabled = fontStep > -2) {
                        Text("A-", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { onFontStep(fontStep + 1) }, enabled = fontStep < 3) {
                        Text("A+", style = MaterialTheme.typography.labelSmall)
                    }
                    if (canTranslate) {
                        IconButton(onClick = onTranslate, enabled = !ui.translating && ui.html != null) {
                            Icon(
                                Icons.Filled.Translate,
                                contentDescription = "Traduci l'articolo",
                                tint = if (ui.translated) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onToggleSave) {
                        Icon(
                            if (ui.saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (ui.saved) "Togli dai salvati" else "Salva per leggere offline",
                            tint = if (ui.saved) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { openExternally(context, ui.article.link) }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Apri nel browser")
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (ui.translating) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ui.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(ui.error, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRetry) { Text("Riprova") }
                        TextButton(onClick = { openExternally(context, ui.article.link) }) {
                            Text("Apri nel browser")
                        }
                    }
                }

                ui.html != null -> ArticleWebView(
                    document = ReaderPage.build(ui, dark, fontStep),
                    baseUrl = ui.article.link
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleWebView(document: String, baseUrl: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.loadsImagesAutomatically = true
                settings.domStorageEnabled = false
                setBackgroundColor(0)
            }
        },
        update = { web ->
            web.loadDataWithBaseURL(baseUrl, document, "text/html", "utf-8", null)
        }
    )
}

/** Il foglio di stile del lettore: stessa palette e stessa tipografia del resto dell'app. */
private object ReaderPage {

    fun build(ui: ReaderUi, dark: Boolean, fontStep: Int): String {
        val bg = if (dark) "#0E1214" else "#FBFAF8"
        val ink = if (dark) "#EDEFEE" else "#11171A"
        val muted = if (dark) "#B3BEC3" else "#4A5459"
        val accent = if (dark) "#8FBDC9" else "#14303C"
        val hair = if (dark) "#2C3438" else "#DDE0DE"
        val size = 17 + fontStep * 1.5

        val title = escape(if (ui.translated) ui.article.shownTitle else ui.article.title)
        val kicker = escape(ui.article.sourceName.uppercase()) +
            (if (ui.offline) " &middot; SALVATO" else "") +
            (if (ui.translated) " &middot; TRADOTTO DA " + ui.article.lang.uppercase() else "")

        return "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
            "<style>" +
            "html,body{margin:0;padding:0;background:" + bg + ";color:" + ink + ";}" +
            "body{padding:20px 20px 60px;font-family:Georgia,serif;font-size:" + size + "px;line-height:1.62;}" +
            ".kicker{font-family:sans-serif;font-size:11px;letter-spacing:.9px;color:" + accent + ";margin-bottom:10px;}" +
            "h1{font-size:1.55em;line-height:1.2;margin:0 0 18px;font-weight:700;letter-spacing:-.4px;}" +
            "h2,h3{font-size:1.15em;line-height:1.3;margin:26px 0 8px;}" +
            "p{margin:0 0 16px;}" +
            "a{color:" + accent + ";text-decoration:none;border-bottom:1px solid " + hair + ";}" +
            "img{max-width:100%;height:auto;display:block;margin:18px 0;border-radius:6px;}" +
            "figure{margin:18px 0;}" +
            "figcaption{font-family:sans-serif;font-size:.72em;color:" + muted + ";margin-top:6px;}" +
            "blockquote{margin:18px 0;padding-left:16px;border-left:3px solid " + hair + ";color:" + muted + ";}" +
            "ul,ol{margin:0 0 16px;padding-left:22px;}li{margin-bottom:6px;}" +
            "hr{border:0;border-top:1px solid " + hair + ";margin:26px 0;}" +
            "pre,code{font-family:monospace;font-size:.85em;white-space:pre-wrap;}" +
            "table{width:100%;border-collapse:collapse;font-size:.85em;}" +
            "td,th{border:1px solid " + hair + ";padding:6px;}" +
            "</style></head><body>" +
            "<div class='kicker'>" + kicker + "</div>" +
            "<h1>" + title + "</h1>" +
            (ui.html ?: "") +
            "</body></html>"
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

private fun openExternally(context: Context, url: String) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        // nessun browser disponibile
    }
}
