# Constellation Pulse

**Constellation Pulse** e un'app rituale di presenza collettiva.

Una volta al giorno, ogni persona sigilla il proprio stato in una sfera di luce.
Il gesto non crea un profilo, un punteggio o un contenuto: crea una traccia.
Alle 20:00, il Chorus apre per un minuto.

> Una volta al giorno, il mondo respira.

## Identita

La sfera e il corpo dell'app.
Il Nearby Field e il campo locale.
Il Chorus e il momento in cui tutto prende senso.

L'app non vuole sembrare un social network, una chat o una meditazione guidata.
Deve sembrare un oggetto rituale: minimale, vivo, anonimo, memorabile.

Le tre frasi guida sono:

- Touch the field.
- Join the Chorus.
- For one minute, you are not alone.

## Stato Attuale

La repo contiene un'app Android funzionante con esperienza visuale avanzata:

- Home con orb procedurale animato e countdown al Chorus quotidiano.
- Rituale giornaliero con 5 segnali emotivi e un messaggio breve opzionale.
- Salvataggio locale di un sigillo al giorno.
- Reveal con orb del giorno, memoria visiva ed echo trace.
- Archivio locale dei sigilli passati.
- Reminder giornaliero prima del Chorus.
- Nearby Field anonimo basato su celle approssimate, senza coordinate precise.
- Firebase Anonymous Auth + Firestore per presenze live ed echo.
- Chorus live backend iniziale con presenze anonime, heartbeat e aggregati visuali.
- Afterglow relic salvata nell'Archive e condivisa su Firestore quando disponibile.
- Echo anonimi tra orb vicini.
- Echo visuale come trasferimento di luce verso l'orb selezionato.
- Memoria giornaliera del coro: presenze, echo inviati, echo ricevuti.
- Cicatrici luminose distinte per echo inviati e ricevuti.
- Orb reattivo al movimento del telefono tramite sensori.
- Stati rituali dell'orb: Dormant, Listening, Contemplative, NearChorus, Sealed, Resonating.
- Micro-interazioni: tap, pressione lunga, attrazione della costellazione interna, inerzia del gesto.
- Chorus mode con fasi Pre-Chorus, Entry, Convergence, The Minute, Afterglow e Sealed.

La parte on-chain esiste come smart contract Solidity minimale, ma non e ancora collegata all'app.

## Esperienza

### Orb

L'orb e procedurale e continuo. Non mostra solo un numero: rivela una forma.

Reagisce a:

- stato emotivo sigillato;
- tempo del giorno;
- avvicinamento al Chorus;
- memoria degli echo;
- movimento del telefono;
- tap e pressione lunga;
- attrazione lenta del dito sulla costellazione interna.

### Nearby Field

Nearby non mostra persone, distanze o nomi.
Mostra presenze anonime come orb vicini, derivati da celle geografiche grossolane.

Le interazioni attuali:

- ascolto di presenze nella cella e nelle celle adiacenti;
- selezione di un orb vicino;
- invio di echo anonimo;
- traiettoria luminosa dell'echo dal proprio orb alla presenza scelta;
- reazione visiva quando arrivano nuove presenze o echo;
- attrazione magnetica dei piccoli orb verso il dito.

### Chorus

Il Chorus e pensato come un'eclissi umana procedurale.

Fasi attuali:

- **Pre-Chorus**: attesa, tensione, countdown rituale.
- **Entry**: ingresso tramite pressione lunga sulla sfera.
- **Convergence**: il campo inizia a popolarsi di presenze anonime.
- **The Minute**: per 60 secondi l'interfaccia si riduce a sfera, campo e presenza.
- **Afterglow**: la costellazione lascia una traccia.
- **Sealed**: il Chorus del giorno e concluso.

Il minuto centrale e legato alle 20:00 locali.

## Struttura Tecnica

- Kotlin
- Jetpack Compose
- Android Gradle project
- Storage JSON locale in app-private storage
- Firebase Anonymous Auth
- Cloud Firestore in `eur3`
- Sensori Android: accelerometro e rotation vector
- Reminder locale Android
- Smart contract Solidity per fase futura Web3

Percorsi principali:

- `android/src/main/java/app/constellationpulse/MainActivity.kt`
- `android/src/main/java/app/constellationpulse/backend/FirebaseFieldService.kt`
- `android/src/main/java/app/constellationpulse/data/ChorusMemoryRepository.kt`
- `FIREBASE_SETUP.md`
- `ROADMAP.md`

## Build Rapida

```bash
cd ~/Scrivania/costellation_pulse
./gradlew :android:assembleDebug
adb install -r android/build/outputs/apk/debug/android-debug.apk
```

Per controllare il telefono:

```bash
adb devices
```

Per avviare l'app da terminale:

```bash
adb shell am start -n app.constellationpulse/.MainActivity
```

## Privacy

Regole di prodotto:

- niente nomi utente;
- niente profili;
- niente feed;
- niente chat;
- niente coordinate raw;
- niente distanza precisa;
- orb ID ruotati giornalmente;
- presenza anonima e temporanea.

## Prossimo Focus

Il prossimo lavoro importante e rendere il Chorus realmente live:

- raffinare presenza globale nel minuto delle 20:00;
- rendere piu robusti conteggio anonimo live, coerenza e sincronizzazione;
- afterglow relic condivisa da rendere piu robusta con aggregazione backend/server-side;
- portare la sensibilita del campo live anche nella Home;
- test con due telefoni alla fine del ciclo.

La versione Apple/iOS e prevista piu avanti. La scelta tecnica confermata e Kotlin Multiplatform per il cuore condiviso + SwiftUI nativa per l'esperienza iPhone.

Vedi `ROADMAP.md` per la lista completa.
