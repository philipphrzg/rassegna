package it.pietro.rassegna.data

/**
 * Fonti proposte alla prima apertura. Sono tutti feed RSS pubblici:
 * niente chiavi API, niente account.
 */
object Catalog {

    val sources: List<Source> = listOf(
        // Prima pagina
        Source("ansa-home", "ANSA", "https://www.ansa.it/sito/ansait_rss.xml", "Prima pagina"),
        Source("post", "Il Post", "https://www.ilpost.it/feed/", "Prima pagina"),
        Source("rep-home", "Repubblica", "https://www.repubblica.it/rss/homepage/rss2.0.xml", "Prima pagina"),
        Source("cds-home", "Corriere della Sera", "https://xml2.corriereobjects.it/rss/homepage.xml", "Prima pagina"),
        Source("fatto", "Il Fatto Quotidiano", "https://www.ilfattoquotidiano.it/feed/", "Prima pagina"),
        Source("indipendente", "L'Indipendente", "https://www.lindipendente.online/feed/", "Prima pagina"),

        // Italia
        Source("ansa-cronaca", "ANSA Cronaca", "https://www.ansa.it/sito/notizie/cronaca/cronaca_rss.xml", "Italia", topic = Topic.CRONACA),
        Source("ansa-politica", "ANSA Politica", "https://www.ansa.it/sito/notizie/politica/politica_rss.xml", "Italia", topic = Topic.POLITICA),
        Source("cds-cronache", "Corriere Cronache", "https://xml2.corriereobjects.it/rss/cronache.xml", "Italia", topic = Topic.CRONACA),

        // Mondo
        Source("ansa-mondo", "ANSA Mondo", "https://www.ansa.it/sito/notizie/mondo/mondo_rss.xml", "Mondo", topic = Topic.MONDO),
        Source("rep-esteri", "Repubblica Esteri", "https://www.repubblica.it/rss/esteri/rss2.0.xml", "Mondo", topic = Topic.MONDO),

        // Stampa internazionale (inglese)
        Source("bbc-world", "BBC News", "https://feeds.bbci.co.uk/news/world/rss.xml", "Stampa internazionale", "en", topic = Topic.MONDO),
        Source("guardian-world", "The Guardian", "https://www.theguardian.com/world/rss", "Stampa internazionale", "en", topic = Topic.MONDO),
        Source("nyt-world", "The New York Times", "https://rss.nytimes.com/services/xml/rss/nyt/World.xml", "Stampa internazionale", "en", topic = Topic.MONDO),
        Source("reuters-world", "Reuters World", "https://news.google.com/rss/search?q=when:24h+site:reuters.com&hl=en-US&gl=US&ceid=US:en", "Stampa internazionale", "en", topic = Topic.MONDO),
        Source("aljazeera", "Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml", "Stampa internazionale", "en"),
        Source("apnews", "Associated Press", "https://news.google.com/rss/search?q=when:24h+site:apnews.com&hl=en-US&gl=US&ceid=US:en", "Stampa internazionale", "en"),
        Source("politico-eu", "Politico Europe", "https://www.politico.eu/feed/", "Stampa internazionale", "en"),
        Source("economist", "The Economist", "https://www.economist.com/international/rss.xml", "Stampa internazionale", "en"),

        // Stampa europea (altre lingue)
        Source("lemonde", "Le Monde", "https://www.lemonde.fr/rss/une.xml", "Stampa europea", "fr"),
        Source("france24", "France 24", "https://www.france24.com/fr/rss", "Stampa europea", "fr"),
        Source("spiegel", "Der Spiegel", "https://www.spiegel.de/schlagzeilen/tops/index.rss", "Stampa europea", "de"),
        Source("dw-de", "Deutsche Welle", "https://rss.dw.com/rdf/rss-de-all", "Stampa europea", "de"),
        Source("elpais", "El Pais", "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/portada", "Stampa europea", "es"),
        Source("eldiario", "elDiario.es", "https://www.eldiario.es/rss/", "Stampa europea", "es"),
        Source("publico-pt", "Publico", "https://feeds.feedburner.com/PublicoRSS", "Stampa europea", "pt"),

        // Economia
        Source("ansa-economia", "ANSA Economia", "https://www.ansa.it/sito/notizie/economia/economia_rss.xml", "Economia", topic = Topic.ECONOMIA),
        Source("sole-economia", "Il Sole 24 Ore", "https://www.ilsole24ore.com/rss/economia.xml", "Economia", topic = Topic.ECONOMIA),
        Source("ft-home", "Financial Times", "https://www.ft.com/rss/home", "Economia", "en", topic = Topic.ECONOMIA),

        // Tecnologia
        Source("ansa-tech", "ANSA Tecnologia", "https://www.ansa.it/sito/notizie/tecnologia/tecnologia_rss.xml", "Tecnologia", topic = Topic.TECNOLOGIA),
        Source("wired-it", "Wired Italia", "https://www.wired.it/feed/rss", "Tecnologia", topic = Topic.TECNOLOGIA),
        Source("dday", "DDAY.it", "https://www.dday.it/rss", "Tecnologia", topic = Topic.TECNOLOGIA),
        Source("hdblog", "HDblog", "https://www.hdblog.it/feed/", "Tecnologia", topic = Topic.TECNOLOGIA),
        Source("verge", "The Verge", "https://www.theverge.com/rss/index.xml", "Tecnologia", "en", topic = Topic.TECNOLOGIA),
        Source("arstechnica", "Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", "Tecnologia", "en", topic = Topic.TECNOLOGIA),
        Source("techcrunch", "TechCrunch", "https://techcrunch.com/feed/", "Tecnologia", "en", topic = Topic.TECNOLOGIA),

        // Scienza
        Source("ansa-scienza", "ANSA Scienza", "https://www.ansa.it/sito/notizie/scienza/scienza_rss.xml", "Scienza", topic = Topic.SCIENZA),
        Source("lescienze", "Le Scienze", "https://www.lescienze.it/rss/all/rss2.0.xml", "Scienza", topic = Topic.SCIENZA),
        Source("natgeo-it", "National Geographic Italia", "https://www.nationalgeographic.it/feed", "Scienza", topic = Topic.SCIENZA),
        Source("nature-news", "Nature News", "https://www.nature.com/nature.rss", "Scienza", "en", topic = Topic.SCIENZA),
        Source("newscientist", "New Scientist", "https://www.newscientist.com/feed/home/", "Scienza", "en", topic = Topic.SCIENZA),

        // Cultura e idee
        Source("ansa-cultura", "ANSA Cultura", "https://www.ansa.it/sito/notizie/cultura/cultura_rss.xml", "Cultura e idee", topic = Topic.CULTURA),
        Source("tascabile", "Il Tascabile", "https://www.iltascabile.com/feed/", "Cultura e idee", topic = Topic.CULTURA),
        Source("doppiozero", "Doppiozero", "https://www.doppiozero.com/rss.xml", "Cultura e idee", topic = Topic.CULTURA),
        Source("aeon", "Aeon", "https://aeon.co/feed.rss", "Cultura e idee", "en", topic = Topic.CULTURA),
        Source("lrb", "London Review of Books", "https://www.lrb.co.uk/feeds/rss", "Cultura e idee", "en", topic = Topic.CULTURA),
        Source("dailynous", "Daily Nous (filosofia)", "https://dailynous.com/feed/", "Cultura e idee", "en", topic = Topic.CULTURA),
        Source("nybooks", "New York Review of Books", "https://www.nybooks.com/feed/", "Cultura e idee", "en", topic = Topic.CULTURA),

        // Scuola
        Source("orizzonte", "Orizzonte Scuola", "https://www.orizzontescuola.it/feed/", "Scuola", topic = Topic.SCUOLA),
        Source("tecnica", "Tecnica della Scuola", "https://www.tecnicadellascuola.it/feed", "Scuola", topic = Topic.SCUOLA),

        // Sport
        Source("gazzetta", "Gazzetta dello Sport", "https://www.gazzetta.it/rss/home.xml", "Sport", topic = Topic.SPORT),
        Source("ansa-sport", "ANSA Sport", "https://www.ansa.it/sito/notizie/sport/sport_rss.xml", "Sport", topic = Topic.SPORT),
        Source("spaziociclismo", "SpazioCiclismo", "https://www.spaziociclismo.it/feed/", "Sport", topic = Topic.SPORT),
        Source("cyclingnews", "Cyclingnews", "https://www.cyclingnews.com/rss/news/", "Sport", "en", topic = Topic.SPORT)
    )

    val defaultSelection: Set<String> = setOf("ansa-home", "post")

    fun byId(id: String): Source? = sources.firstOrNull { it.id == id }
}
