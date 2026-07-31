# Rassegna

Aggregatore di notizie per Android. Alla prima apertura scegli le fonti da una lista;
puoi cambiarle in qualsiasi momento dal pulsante Fonti in alto a destra.

## Cosa fa

- Un'unica lista di articoli, dai piu' recenti ai piu' vecchi, presi dai feed RSS delle fonti attive
- Barra delle sezioni in alto: Politica, Mondo, Cronaca, Economia, Tecnologia, Scienza, Cultura, Scuola, Sport, Varie.
  Compaiono solo le sezioni che in quel momento hanno articoli.
- Onboarding alla prima installazione: fonti raggruppate per tema (Prima pagina, Italia, Mondo, Economia, Tecnologia, Scienza, Cultura e idee, Scuola, Sport)
- Schermata Fonti per attivare e disattivare le fonti in seguito
- Fonti estere (BBC, Guardian, NYT, Al Jazeera, Le Monde, Der Spiegel, El Pais, Politico, Nature, LRB...) tradotte in italiano
- La traduzione avviene sul telefono con ML Kit: il dizionario di ogni lingua si scarica una volta (circa 30 MB) e poi funziona anche offline. Nessuna chiave API, nessun costo.
- Su ogni articolo tradotto c'e' "Mostra l'originale" per leggere il testo di partenza
- L'interruttore "Traduci in italiano" sta in cima alla schermata Fonti
- Aggiunta di feed personalizzati: incolli l'indirizzo RSS, scegli la lingua, l'app lo verifica prima di salvarlo
- **Lettura dentro l'app**: toccando un articolo la pagina viene scaricata, ripulita (porta Kotlin
  di Readability di Mozilla) e reimpaginata con la grafica dell'app. Niente pubblicita', niente banner
  dei cookie, niente JavaScript.
- **Salvataggio a scelta tua**: il segnalibro accanto a ogni articolo lo scarica e lo tiene sul telefono.
  La schermata "Salvati" si legge anche in aereo. Il segnalibro premuto una seconda volta cancella il file.
- Nel lettore: A-/A+ per la dimensione del testo, il pulsante traduci per voltare in italiano
  l'articolo intero (non solo il titolo) e, se un sito proprio non si lascia estrarre, l'apertura nel browser
- Le scelte restano salvate sul telefono (DataStore). Nessun account, nessuna chiave API, nessun server.

## Come ottenere l'APK

### Opzione A - GitHub Actions (senza installare niente)

1. Crea un repository su GitHub e carica questa cartella.
2. Vai su **Actions** e lancia il workflow **Build APK** (parte anche da solo al primo push).
3. A fine build scarica l'artifact `rassegna-apk`: dentro c'e' `app-debug.apk`.
4. Passa l'APK al telefono e installalo abilitando "Installa app sconosciute".

### Opzione B - Android Studio

1. Apri la cartella con Android Studio (Giraffe o piu' recente).
2. Attendi la sincronizzazione Gradle.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).

## Modificare la lista delle fonti

Il catalogo iniziale sta in `app/src/main/java/it/pietro/rassegna/data/Catalog.kt`.
Ogni riga e' una fonte:

```kotlin
Source("id-univoco", "Nome mostrato", "https://indirizzo/feed.xml", "Categoria", "en")
// l'ultimo campo e' la lingua: si puo' omettere per le fonti italiane
```

Le categorie si creano da sole: basta scrivere un nome nuovo nell'ultimo campo.
Se un feed cambia indirizzo, l'app lo segnala in cima alla lista ("Non hanno risposto: ...")
invece di bloccarsi.

## Note

- Gli indirizzi RSS delle testate cambiano ogni tanto. Quelli che non rispondono si possono
  togliere dal catalogo o sostituire con l'aggiunta manuale dentro l'app.
- La build di GitHub Actions produce un APK di debug, firmato con la chiave di debug:
  va bene per installarlo a mano, non per il Play Store.

## Traduzione: cosa sapere

- Lingue verso l'italiano: inglese, francese, tedesco, spagnolo, portoghese e le altre supportate da ML Kit.
- Il download del dizionario passa dai servizi Google Play, quindi serve un telefono che li abbia.
- Vengono tradotti titolo e sommario, non l'articolo intero: quando apri il link leggi la pagina originale.
  Se ti serve anche quella tradotta, il browser interno di Chrome propone la traduzione della pagina.

## Come vengono divisi gli articoli per sezione

`Topic.kt` contiene le sezioni e il classificatore, che decide in quest'ordine:

1. **La fonte e' monotematica**: ANSA Politica, Gazzetta, Nature, Il Tascabile e simili hanno
   gia' un `topic` fisso nel catalogo, e vale per tutti i loro articoli.
2. **Le etichette del feed**: molti RSS portano dentro un `<category>` (politics, sport, kultur...),
   mappato sulle sezioni italiane.
3. **Le parole del titolo e del sommario**: un elenco di termini per sezione, in italiano, inglese,
   francese, tedesco e spagnolo. Vince la sezione con piu' corrispondenze.
4. Se non decide nulla, l'articolo finisce in **Varie**; se poi arriva la traduzione, viene
   riclassificato sul titolo italiano.

Per correggere gli errori di classificazione basta aggiungere parole alla lista della sezione
in `Classifier.keywords`, oppure fissare il tema di una fonte nel catalogo:

```kotlin
Source("mia-fonte", "Nome", "https://.../feed.xml", "Categoria", "en", topic = Topic.CULTURA)
```

## Lettura offline: come funziona

- Se il feed porta gia' con se' l'articolo intero (`content:encoded`, tipico di WordPress: Il Post,
  Wired, Il Fatto, Il Tascabile...) l'app non scarica nulla e impagina quello.
- Altrimenti scarica la pagina e la passa a Readability4J, che tiene il testo e butta il resto.
- Gli articoli salvati finiscono in `filesDir/articoli` come HTML gia' pulito, e l'elenco sta
  nelle preferenze: restano dopo il riavvio e non scadono finche' non li togli.
- Qualche sito (soprattutto i paywall duri, tipo FT) non si lascia estrarre: in quel caso compare
  un messaggio con il pulsante per aprirlo nel browser.
- La traduzione dell'articolo intero avviene paragrafo per paragrafo, sempre sul telefono: su un
  pezzo lungo ci mette qualche secondo.
