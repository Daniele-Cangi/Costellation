# Firebase Setup

Firebase e usato per rendere vivo il campo anonimo dell'app.

Oggi alimenta il Nearby Field e gli echo.
Il prossimo passo sara usarlo anche per rendere il Chorus un evento live globale.

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

## Backend Da Fare Per Il Chorus

Il Chorus ora esiste come esperienza visuale locale/time-aware.
Per renderlo fondativo serve collegarlo a presenza live reale.

Proposta struttura:

```text
dailyChoruses/{day}
dailyChoruses/{day}/presences/{presenceId}
dailyChoruses/{day}/minutes/{minuteKey}/presences/{presenceId}
dailyChoruses/{day}/afterglow/{presenceId}
```

Campi consigliati per `presences`:

```text
orbId
joinedAt
lastSeenAt
coarseCellId
touchStability
motionSignature
stillness
localDensity
clientSeed
```

Campi aggregati da derivare:

```text
globalPresenceCount
localFieldDensity
synchronizationLevel
coherence
turbulence
afterglowSeed
```

Nota importante: questi valori non devono essere mostrati come numeri tecnici.
Devono diventare comportamento della sfera.

## Roadmap Firebase

1. Aggiungere scrittura presenza live nel minuto Chorus.
2. Aggiornare heartbeat presenza ogni pochi secondi durante il minuto.
3. Rimuovere o ignorare presenze scadute.
4. Calcolare aggregati anonimi lato client o Cloud Function.
5. Usare gli aggregati per guidare `ChorusEclipseField`.
6. Salvare afterglow giornaliero come reliquia visuale.
7. Testare con due telefoni solo alla fine del ciclo.

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
