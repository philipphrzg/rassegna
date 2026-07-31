package it.pietro.rassegna.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rassegna")

class Prefs(private val context: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val SELECTED = stringSetPreferencesKey("selected")
        val CUSTOM = stringSetPreferencesKey("custom")
        val TRANSLATE = booleanPreferencesKey("translate")
        val SAVED = stringSetPreferencesKey("saved")
        val FONT = intPreferencesKey("font_step")
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }

    val translate: Flow<Boolean> = context.dataStore.data.map { it[Keys.TRANSLATE] ?: true }

    val fontStep: Flow<Int> = context.dataStore.data.map { it[Keys.FONT] ?: 0 }

    val savedArticles: Flow<List<Article>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.SAVED] ?: emptySet()).mapNotNull { Article.decodeSaved(it) }
            .sortedByDescending { it.publishedAt }
    }

    val selectedIds: Flow<Set<String>> = context.dataStore.data.map {
        it[Keys.SELECTED] ?: emptySet()
    }

    val customSources: Flow<List<Source>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.CUSTOM] ?: emptySet()).mapNotNull { Source.decode(it) }
    }

    suspend fun setSelected(ids: Set<String>) {
        context.dataStore.edit { it[Keys.SELECTED] = ids }
    }

    suspend fun setTranslate(value: Boolean) {
        context.dataStore.edit { it[Keys.TRANSLATE] = value }
    }

    suspend fun setFontStep(value: Int) {
        context.dataStore.edit { it[Keys.FONT] = value.coerceIn(-2, 3) }
    }

    suspend fun addSaved(article: Article) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.SAVED] ?: emptySet())
                .filterNot { it.startsWith(article.link + "\u0001") }
                .toMutableSet()
            current.add(article.encodeSaved())
            prefs[Keys.SAVED] = current
        }
    }

    suspend fun removeSaved(link: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED] = (prefs[Keys.SAVED] ?: emptySet())
                .filterNot { it.startsWith(link + "\u0001") }
                .toSet()
        }
    }

    suspend fun setOnboarded() {
        context.dataStore.edit { it[Keys.ONBOARDED] = true }
    }

    suspend fun addCustom(source: Source) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.CUSTOM] ?: emptySet()).toMutableSet()
            current.add(source.encode())
            prefs[Keys.CUSTOM] = current
            val sel = (prefs[Keys.SELECTED] ?: emptySet()).toMutableSet()
            sel.add(source.id)
            prefs[Keys.SELECTED] = sel
        }
    }

    suspend fun removeCustom(id: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.CUSTOM] ?: emptySet())
                .filterNot { Source.decode(it)?.id == id }
                .toSet()
            prefs[Keys.CUSTOM] = current
            prefs[Keys.SELECTED] = (prefs[Keys.SELECTED] ?: emptySet()) - id
        }
    }
}
