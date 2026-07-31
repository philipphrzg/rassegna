package it.pietro.rassegna.data

import java.util.Locale

/** Le sezioni della rassegna. L'ordine e' quello in cui compaiono in alto. */
enum class Topic(val label: String) {
    POLITICA("Politica"),
    MONDO("Mondo"),
    CRONACA("Cronaca"),
    ECONOMIA("Economia"),
    TECNOLOGIA("Tecnologia"),
    SCIENZA("Scienza"),
    CULTURA("Cultura"),
    SCUOLA("Scuola"),
    SPORT("Sport"),
    ALTRO("Varie")
}

/**
 * Assegna una sezione a ogni articolo. In ordine di affidabilita':
 * 1. la fonte e' monotematica (ANSA Politica, Gazzetta, Nature...)
 * 2. le etichette <category> che il feed porta con se'
 * 3. le parole del titolo e del sommario, in cinque lingue
 */
object Classifier {

    private val keywords: Map<Topic, List<String>> = mapOf(
        Topic.SPORT to listOf(
            "calcio", "serie a", "champions", "juventus", "inter", "milan", "napoli", "roma calcio",
            "allenatore", "gol", "scudetto", "campionato", "tennis", "atp", "wta", "formula 1",
            "motogp", "ciclismo", "giro d'italia", "tour de france", "vuelta", "tappa", "corsa",
            "basket", "nba", "volley", "olimpiadi", "mondiali", "nazionale", "atleta",
            "football", "premier league", "match", "coach", "striker", "cyclist", "cycling", "race",
            "olympics", "tournament", "championship", "fussball", "bundesliga", "futbol", "liga",
            "deporte", "sport", "equipe", "cyclisme"
        ),
        Topic.SCIENZA to listOf(
            "ricerca", "ricercatori", "studio pubblicato", "scienziati", "clima", "climatico",
            "spazio", "nasa", "esa", "asteroide", "pianeta", "telescopio", "vaccino", "cellule",
            "dna", "genoma", "fisica", "chimica", "biologia", "specie", "fossile", "archeolog",
            "cervello", "neuro", "science", "researchers", "study", "climate", "space", "planet",
            "telescope", "physics", "biology", "species", "brain", "wissenschaft", "forschung",
            "ciencia", "estudio", "recherche", "chercheurs"
        ),
        Topic.TECNOLOGIA to listOf(
            "intelligenza artificiale", "algoritmo", "app", "smartphone", "iphone", "android",
            "google", "apple", "microsoft", "meta", "openai", "chatgpt", "software", "chip",
            "processore", "startup tech", "internet", "social network", "privacy", "hacker",
            "cybersicurezza", "computer", "digitale", "artificial intelligence", "ai model",
            "chatbot", "semiconductor", "cyber", "data breach", "tecnologia", "technology",
            "kuenstliche intelligenz", "inteligencia artificial", "numerique"
        ),
        Topic.ECONOMIA to listOf(
            "borsa", "spread", "pil", "inflazione", "tassi", "bce", "banca centrale", "mercati",
            "azioni", "obbligazioni", "conti pubblici", "manovra", "fisco", "tasse", "imprese",
            "occupazione", "disoccupazione", "salari", "contratto di lavoro", "petrolio", "gas",
            "dazi", "economia", "economy", "inflation", "markets", "stocks", "interest rates",
            "central bank", "tariffs", "trade deal", "wirtschaft", "boerse", "economia", "bolsa",
            "impuestos", "croissance", "chomage"
        ),
        Topic.CULTURA to listOf(
            "libro", "libri", "romanzo", "scrittore", "scrittrice", "poesia", "editore",
            "mostra", "museo", "arte", "pittore", "teatro", "cinema", "film", "regista",
            "festival", "musica", "concerto", "album", "filosofia", "filosofo", "storia",
            "storico", "archivio", "letteratura", "book", "novel", "writer", "poetry",
            "museum", "exhibition", "art", "theatre", "movie", "director", "philosophy",
            "literature", "kultur", "kunst", "buch", "cultura", "libro", "litterature", "cinema"
        ),
        Topic.SCUOLA to listOf(
            "scuola", "docenti", "insegnanti", "studenti", "maturita", "concorso scuola",
            "ministero dell'istruzione", "universita", "atenei", "supplenze", "graduatorie",
            "didattica", "school", "teachers", "students", "university", "education",
            "schule", "lehrer", "escuela", "profesores", "ecole", "enseignants"
        ),
        Topic.POLITICA to listOf(
            "governo", "premier", "meloni", "parlamento", "camera", "senato", "ministro",
            "opposizione", "partito", "elezioni", "voto", "referendum", "coalizione", "riforma",
            "decreto", "legge", "quirinale", "sindaco", "regione", "consiglio dei ministri",
            "government", "parliament", "minister", "election", "vote", "senate", "congress",
            "president", "policy", "bill", "regierung", "wahl", "bundestag", "gobierno",
            "elecciones", "gouvernement", "elections", "assemblee nationale"
        ),
        Topic.MONDO to listOf(
            "ucraina", "russia", "putin", "zelensky", "gaza", "israele", "hamas", "medio oriente",
            "cina", "pechino", "stati uniti", "washington", "casa bianca", "trump", "nato",
            "unione europea", "bruxelles", "onu", "guerra", "tregua", "profughi", "migranti",
            "ukraine", "moscow", "china", "beijing", "white house", "european union", "brussels",
            "united nations", "war", "ceasefire", "refugees", "krieg", "guerra", "guerre"
        ),
        Topic.CRONACA to listOf(
            "arrestato", "arresto", "omicidio", "indagine", "procura", "carabinieri", "polizia",
            "processo", "condannato", "incidente", "incendio", "terremoto", "alluvione",
            "maltempo", "soccorsi", "vigili del fuoco", "rapina", "truffa", "femminicidio",
            "arrested", "police", "investigation", "trial", "crash", "fire", "earthquake",
            "flood", "rescue", "polizei", "policia", "incendio", "accidente", "enquete"
        )
    )

    private val tagMap: Map<String, Topic> = mapOf(
        "politica" to Topic.POLITICA, "politics" to Topic.POLITICA, "politik" to Topic.POLITICA,
        "politique" to Topic.POLITICA, "elezioni" to Topic.POLITICA,
        "mondo" to Topic.MONDO, "world" to Topic.MONDO, "esteri" to Topic.MONDO,
        "international" to Topic.MONDO, "internacional" to Topic.MONDO, "ausland" to Topic.MONDO,
        "cronaca" to Topic.CRONACA, "crime" to Topic.CRONACA, "giustizia" to Topic.CRONACA,
        "economia" to Topic.ECONOMIA, "business" to Topic.ECONOMIA, "economy" to Topic.ECONOMIA,
        "finance" to Topic.ECONOMIA, "markets" to Topic.ECONOMIA, "wirtschaft" to Topic.ECONOMIA,
        "tecnologia" to Topic.TECNOLOGIA, "technology" to Topic.TECNOLOGIA, "tech" to Topic.TECNOLOGIA,
        "digital" to Topic.TECNOLOGIA, "internet" to Topic.TECNOLOGIA,
        "scienza" to Topic.SCIENZA, "science" to Topic.SCIENZA, "health" to Topic.SCIENZA,
        "salute" to Topic.SCIENZA, "environment" to Topic.SCIENZA, "ambiente" to Topic.SCIENZA,
        "cultura" to Topic.CULTURA, "culture" to Topic.CULTURA, "books" to Topic.CULTURA,
        "libri" to Topic.CULTURA, "arts" to Topic.CULTURA, "spettacoli" to Topic.CULTURA,
        "music" to Topic.CULTURA, "film" to Topic.CULTURA, "kultur" to Topic.CULTURA,
        "scuola" to Topic.SCUOLA, "education" to Topic.SCUOLA, "universita" to Topic.SCUOLA,
        "sport" to Topic.SPORT, "sports" to Topic.SPORT, "calcio" to Topic.SPORT,
        "football" to Topic.SPORT, "deportes" to Topic.SPORT
    )

    fun classify(source: Source, title: String, summary: String, tags: List<String>): Topic {
        source.topic?.let { return it }

        for (tag in tags) {
            val clean = tag.lowercase(Locale.ROOT).trim()
            tagMap[clean]?.let { return it }
            tagMap.entries.firstOrNull { clean.contains(it.key) }?.let { return it.value }
        }

        return fromText(title + " " + summary)
    }

    /** Usata anche dopo la traduzione, quando il titolo diventa italiano. */
    fun fromText(text: String): Topic {
        val haystack = text.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}' ]"), " ")
        var best = Topic.ALTRO
        var bestScore = 0
        for ((topic, words) in keywords) {
            var score = 0
            for (w in words) if (haystack.contains(w)) score++
            if (score > bestScore) {
                bestScore = score
                best = topic
            }
        }
        return best
    }
}
