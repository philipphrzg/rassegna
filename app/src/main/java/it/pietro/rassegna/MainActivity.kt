package it.pietro.rassegna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import it.pietro.rassegna.ui.ArticleScreen
import it.pietro.rassegna.ui.FeedScreen
import it.pietro.rassegna.ui.NewsViewModel
import it.pietro.rassegna.ui.OnboardingScreen
import it.pietro.rassegna.ui.RassegnaTheme
import it.pietro.rassegna.ui.SourcesScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RassegnaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot(vm: NewsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var showSources by remember { mutableStateOf(false) }

    when {
        !state.ready -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        !state.onboarded -> OnboardingScreen(
            sources = state.allSources,
            selected = state.selected,
            onToggle = vm::toggle,
            onDone = vm::finishOnboarding
        )

        state.reader != null -> {
            BackHandler { vm.closeReader() }
            ArticleScreen(
                ui = state.reader!!,
                fontStep = state.fontStep,
                onBack = vm::closeReader,
                onToggleSave = vm::toggleSaveCurrent,
                onTranslate = vm::translateCurrent,
                onFontStep = vm::setFontStep,
                onRetry = vm::retryArticle
            )
        }

        showSources -> SourcesScreen(
            sources = state.allSources,
            selected = state.selected,
            translate = state.translate,
            onToggle = vm::toggle,
            onTranslateChange = vm::setTranslate,
            onRemoveCustom = vm::removeCustomSource,
            onAddCustom = { name, url, lang -> vm.addCustomSource(name, url, lang) },
            onBack = {
                showSources = false
                vm.refresh()
            }
        )

        else -> FeedScreen(
            articles = state.displayedArticles,
            topics = state.availableTopics,
            selectedTopic = state.topic,
            savedLinks = state.savedLinks,
            savingLinks = state.savingLinks,
            showSaved = state.showSaved,
            hasMore = state.hasMore,
            loading = state.loading,
            translating = state.translating,
            failed = state.failedSources,
            notice = state.notice,
            onTopicChange = vm::setTopic,
            onOpenArticle = vm::openArticle,
            onToggleSave = vm::toggleSaveFromList,
            onShowSaved = vm::showSaved,
            onLoadMore = vm::loadMore,
            onRefresh = vm::refresh,
            onOpenSources = { showSources = true }
        )
    }
}
