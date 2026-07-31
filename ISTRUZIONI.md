# Come ottenere l'APK in 5 minuti

Serve solo un account GitHub, gratuito. Non devi installare niente sul computer.

## 1. Crea il repository

1. Vai su https://github.com/new
2. Nome: `rassegna` (o quello che preferisci)
3. Scegli **Public** (i minuti di build sono illimitati sui repo pubblici; su quelli privati
   ne hai comunque 2000 al mese, e a noi ne servono circa 4 per build)
4. **Non** aggiungere README o .gitignore: la cartella li ha gia'
5. Crea il repository

## 2. Carica i file

Nella pagina che si apre clicca **uploading an existing file**, poi trascina dentro
tutto il contenuto della cartella `rassegna` (non la cartella: il suo contenuto,
cioe' `app`, `.github`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`,
`README.md`, `.gitignore`).

Attenzione: il caricamento da browser a volte ignora le cartelle che iniziano con un punto.
Se dopo l'upload non vedi `.github`, creala a mano: **Add file > Create new file**, scrivi nel
nome `.github/workflows/build.yml` e incolla dentro il contenuto di quel file.

Poi conferma con **Commit changes**.

## 3. Aspetta la build

1. Vai sulla scheda **Actions** del repository
2. Vedrai la build partita da sola (si chiama "Build APK"). Se non e' partita, aprila
   dalla lista a sinistra e premi **Run workflow**
3. Dura circa 4-6 minuti la prima volta

## 4. Scarica l'APK sul telefono

Vai sulla scheda **Releases** del repository (colonna di destra nella pagina principale),
apri l'ultima e scarica **rassegna.apk**. Il link funziona anche dal browser del telefono.

Sul telefono: apri il file scaricato, Android chiedera' il permesso di installare app da
fonti sconosciute per quel browser. Concedilo e installa.

## Se la build fallisce

Nella scheda Actions apri la build rossa e guarda il passaggio segnato in rosso: il messaggio
di errore e' nelle ultime righe. Copiamelo e lo sistemiamo.

Le cause piu' comuni sono banali: la cartella `.github` non caricata (vedi punto 2), oppure
i file caricati dentro una sottocartella invece che nella radice del repository. Nella pagina
principale del repo devi vedere `app`, `build.gradle.kts` e `settings.gradle.kts` al primo livello.

## In alternativa: Android Studio

1. Scarica Android Studio da https://developer.android.com/studio
2. **Open** e scegli la cartella `rassegna`
3. Aspetta la sincronizzazione Gradle (la prima volta scarica circa 1 GB)
4. **Build > Build Bundle(s) / APK(s) > Build APK(s)**
5. L'APK finisce in `app/build/outputs/apk/debug/`
