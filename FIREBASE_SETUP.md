# Firebase Setup

Firebase e usato per rendere vivo il campo anonimo dell'app.

Oggi alimenta il Nearby Field, gli echo e una prima versione del Chorus live.

## Progetto

- Firebase project ID: `constellation-pulse`
- Android package: `app.constellationpulse`
- Config file: `android/google-services.json`
- Firestore database: `(default)`
- Firestore location: `eur3`
- Firestore rules: `firebase.firestore.rules`
- Authentication: Anonymous enabled

Console:

```text
https://console.firebase.google.com/project/constellation-pulse
```

Provider auth:

```text
https://console.firebase.google.com/project/constellation-pulse/authentication/providers
```

Verificare che **Anonymous** sia abilitato.

## Comandi Utili

Se Firebase CLI e configurato:

```bash
firebase use
firebase firestore:databases:get '(default)' --project constellation-pulse
firebase deploy --only firestore:rules --project constellation-pulse
```

Build e installazione Android:

```bash
cd ~/Scrivania/costellation_pulse
./gradlew :android:assembleDebug
adb install -r android/build/outputs/apk/debug/android-debug.apk
```

Avvio e log:

```bash
adb shell am start -n app.constellationpulse/.MainActivity
adb logcat -d -t 1000 | rg -i 'FATAL EXCEPTION|AndroidRuntime|Firebase|Firestore|constellationpulse'
```

## Comportamento Senza Firebase

L'app deve restare apribile anche senza `android/google-services.json`.

Quando il file non esiste:

- il plugin Google Services non viene applicato;
- il Nearby Field usa un campo procedurale locale;
- l'app resta navigabile;
- non vengono pubblicate presenze live;
- gli echo restano locali.

Quando il file esiste:

- Firebase viene inizializzato;
- Auth anonimo crea/usa un'identita temporanea;
- Firestore riceve presenze ed echo;
- Nearby Field ascolta la cella corrente e le celle adiacenti.

## Struttura Firestore Attuale

```text
dailyFields/{day}/cells/{cellId}/orbs/{orbId}
dailyFields/{day}/cells/{cellId}/echoes/{echoId}
dailyChoruses/{day}/presences/{presenceId}
dailyChoruses/{day}/afterglow/relic
```

Esempio:

```text
dailyFields/20260625/cells/cell_559_126/orbs/{dailyOrbId}
dailyFields/20260625/cells/cell_559_126/echoes/{echoId}
```

Gli orb remoti vengono deduplicati per `orbId` quando sono ascoltate piu celle.

## Privacy

Regole da mantenere:

- non salvare latitudine/longitudine raw;
- usare solo celle approssimate;
- ruotare gli orb ID ogni giorno;
- non salvare username;
- non salvare profili;
- non salvare chat;
- non mostrare distanza precisa;
- tenere l'interazione anonima e temporanea.

## Funzioni Gia Implementate

- Anonymous Auth.
- Pubblicazione dell'orb giornaliero nella cella anonima.
- Ascolto live della cella corrente e delle 8 celle vicine.
- Deduplica presenze remote.
- Invio echo a un orb selezionato.
- Ascolto echo ricevuti.
- Memoria locale del coro giornaliero:
  - picco presenze;
  - echo inviati;
  - echo ricevuti;
  - mood sintetico del campo.
- Presenza live nel Chorus:
  - presence ID anonimo giornaliero;
  - heartbeat durante la partecipazione;
  - touch stability;
  - stillness;
  - turbulence;
  - coarse cell ID;
  - client seed procedurale.
- Afterglow relic generata dagli aggregati live disponibili.
- Afterglow relic condivisa su `dailyChoruses/{day}/afterglow/relic`.

## Backend Da Fare Per Il Chorus

Il Chorus ora ha una prima presenza live reale.
La versione attuale scrive presenze anonime, ascolta gli ultimi heartbeat attivi e salva una reliquia condivisa.
Gli aggregati sono ancora calcolati lato client e guidano `ChorusEclipseField`.

Struttura attuale:

```text
dailyChoruses/{day}/presences/{presenceId}
dailyChoruses/{day}/afterglow/relic
```

Campi consigliati per `presences`:

```text
presenceId
day
coarseCellId
touchStability
stillness
turbulence
clientSeed
joinedAtMillis
lastSeenAtMillis
```

Aggregati derivati lato client:

```text
globalPresenceCount
localFieldDensity
synchronizationLevel
coherence
turbulence
afterglowSeed
```

Campi attuali per `afterglow/relic`:

```text
day
afterglowSeed
globalPresenceCount
localFieldDensity
synchronizationLevel
coherence
turbulence
sealedAtMillis
sealedBy
```

Nota importante: questi valori non devono essere mostrati come numeri tecnici.
Devono diventare comportamento della sfera.

## Roadmap Firebase

1. Raffinare heartbeat: solo finestre temporali realmente rilevanti.
2. Aggiungere `minutes/{minuteKey}` se serve una separazione piu pulita del minuto centrale.
3. Valutare Cloud Function per aggregati anonimi server-side.
4. Rendere afterglow relic server-derived con Cloud Function o transazione piu robusta.
5. Rifinire rendering della reliquia nell'Archive.
6. Testare con due telefoni solo alla fine del ciclo.

## Troubleshooting

Controllare device:

```bash
adb devices
```

Reinstallare APK:

```bash
adb install -r android/build/outputs/apk/debug/android-debug.apk
```

Se `adb` non esiste:

```bash
sudo apt update
sudo apt install -y adb
```

Se Java/Gradle non partono, questa macchina usa:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```
