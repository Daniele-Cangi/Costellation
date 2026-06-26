# Roadmap

Questo documento tiene insieme il lavoro fatto e le prossime mosse.

La direzione e chiara: **Constellation Pulse deve diventare un oggetto rituale, minimale ma avanzato**.

## Fatto

### Fondazione Android

- App Kotlin/Jetpack Compose.
- Navigazione Home, Ritual, Ceremony, Resonance, Reveal, Nearby, History, Chorus.
- Storage locale dei sigilli giornalieri.
- Archivio dei sigilli.
- Reminder giornaliero.
- Build debug funzionante e installazione via ADB.

### Orb

- Orb procedurale basato sul sigillo del giorno.
- Movimento continuo senza reset visibile del ritmo.
- Colore e tono legati ai parametri emotivi.
- Reazione al movimento del telefono.
- Pulsazione lenta e piu solenne.
- Stati rituali:
  - Dormant
  - Listening
  - Contemplative
  - NearChorus
  - Sealed
  - Resonating
- Tap leggero con onda interna.
- Pressione lunga con apertura del nucleo.
- Attrazione della costellazione interna verso il dito.
- Movimento del dito filtrato con inerzia.
- Nodi con resistenza procedurale: non tutti vengono catturati.
- Tracce visive dagli echo giornalieri.
- Sfumatura e tensione della Home quando il campo live vicino non e vuoto.
- Shake poetico con disturbo breve e ricomposizione lenta.

### Nearby Field

- Permesso posizione approssimativa.
- Celle anonime, nessuna coordinata raw.
- Ascolto cella corrente + celle adiacenti.
- Presenze remote anonime come orb.
- Echo anonimo verso un orb selezionato.
- Echo visuale come trasferimento di luce dal proprio orb alla presenza scelta.
- L'orb ricevente reagisce temporaneamente al passaggio dell'echo.
- Memoria locale: presenze, echo inviati, echo ricevuti.
- Cicatrici luminose diverse per echo inviati e ricevuti.
- Reazione visiva quando il campo cambia.
- Attrazione magnetica dei piccoli orb verso il dito.

### Firebase

- Firebase project `constellation-pulse`.
- Android app `app.constellationpulse`.
- `google-services.json` collegato.
- Anonymous Auth.
- Firestore in `eur3`.
- Regole Firestore deployate.
- Presenze ed echo live nel Nearby Field.
- Presenze live iniziali nel Chorus.
- Heartbeat anonimo durante la partecipazione.
- Aggregati client-side per conteggio, densita locale, coerenza e turbolenza.
- Afterglow relic salvata localmente e condivisa su Firestore.

### Chorus

- Nuova schermata Chorus.
- Ingresso dalla Home con long press sull'orb.
- Ingresso anche tramite `Join the Chorus`.
- Fasi rituali:
  - Pre-Chorus
  - Entry
  - Convergence
  - The Minute
  - Afterglow
  - Sealed
- Campo procedurale attorno alla sfera.
- Presenze anonime come luci, non avatar.
- Campo guidato da presenze live quando Firebase e disponibile.
- Minuto centrale legato alle 20:00 locali.
- Afterglow dopo il minuto.
- Reliquia visuale nell'Archive.
- Copy minimale:
  - The Chorus opens soon.
  - Touch the field.
  - The field is no longer empty.
  - For one minute, you are not alone.
  - You were part of today's Chorus.
  - Today's Chorus is sealed.

## Prossime Mosse Migliori

### 1. Raffinare Chorus Live

Il Chorus ha ora una prima presenza live reale.
Il prossimo passo e renderla piu precisa, piu robusta e piu memorabile.

Da fare:

- limitare e rifinire heartbeat sulle finestre temporali giuste;
- valutare struttura `minutes/{minuteKey}` per il minuto centrale;
- migliorare calcolo `coherence` e `synchronizationLevel`;
- salvare un `afterglowSeed` condiviso;
- valutare Cloud Function per aggregati server-side;
- mostrare differenze tra campo globale e campo locale senza numeri tecnici.

Obiettivo: l'utente deve percepire che non sta guardando un'animazione, ma un evento generato da presenze vere.

### 2. Raffinare Afterglow Come Reliquia

Alla fine del Chorus, il campo lascia gia una prima traccia salvata e condivisa.
Ora va resa piu forte, piu condivisa e piu riconoscibile.

Da fare:

- derivare la reliquia da backend/server-side, non solo da proposta client;
- migliorare `afterglowSeed` con presenza globale e coerenza reale;
- rendere l'Archive piu reliquiario e meno lista;
- distinguere ancora meglio sigillo personale e Chorus relic;
- evitare numeri tecnici.

Messaggio chiave:

```text
You were part of today's Chorus.
```

### 3. Echo Come Trasferimento Di Luce

Prima fase completata: l'echo ora ha un corpo visivo.

Fatto:

- animare una traiettoria luminosa dal tuo orb a quello selezionato;
- far cambiare temporaneamente il colore dell'orb ricevente;
- lasciare cicatrice diversa per echo inviato e ricevuto;

Da fare:

- aggiungere haptic morbido al rilascio.
- rendere l'arrivo dell'echo remoto piu evidente quando un altro telefono risponde.

### 4. Reazione Degli Altri Sulla Home

Prima fase completata: la Home ora sente il mondo anche fuori dal Nearby Field.

Fatto:

- ascoltare un segnale leggero di presenza live;
- deformare appena l'orb quando il campo non e vuoto;
- sfumare il colore verso il mood del campo;
- usare linee di tensione molto sottili.

Da fare:

- rendere il segnale ancora meno testuale;
- calibrare intensita e durata con test su telefono reale;
- valutare un fallback locale se Firebase non e disponibile.

### 5. Shake Poetico

Prima fase completata: il movimento deciso del telefono ora produce un disturbo poetico e poi ricompone lentamente l'orb.

Fatto:

- rilevare shake leggero;
- creare disturbo visuale breve;
- ricomporre lentamente la costellazione;
- evitare effetto giocattolo.

Da fare:

- calibrare soglia e cooldown su piu telefoni reali;
- rendere il feedback haptic piu morbido quando avremo una scelta finale;
- verificare che non si attivi durante uso normale in camminata.

### 6. Audio Opzionale

Da fare solo quando l'esperienza visuale e stabile.

Principi:

- opzionale;
- molto basso;
- niente musica invadente;
- micro-tono o drone ambient;
- silenzio come default rispettabile.

### 7. Qualita E Release

Da fare:

- test unitari per modello dati e repository;
- test mirati per generazione sigilli;
- controlli su permessi e fallback offline;
- configurazione release APK/AAB;
- icona finale;
- privacy text minimale;
- test finale con due telefoni.

### 8. Apple / iOS

Non e una fase da fare ora.
Va pianificata dopo Chorus live, afterglow, stabilizzazione Android e primi test reali.

Scelta tecnica confermata:

```text
Kotlin Multiplatform per il cuore condiviso
+ SwiftUI nativa per l'esperienza iOS
```

Perche questa scelta:

- mantiene condivisa la logica importante;
- evita di riscrivere due volte generazione sigilli, stati rituali, memoria e regole Chorus;
- permette su iPhone un'interfaccia davvero Apple;
- sfrutta bene CoreMotion, haptics iOS, notifiche locali e Firebase iOS;
- evita una migrazione totale a Flutter/React Native, che rischierebbe di appiattire il feeling visuale dell'orb.

Da condividere con Kotlin Multiplatform:

- modelli dati;
- generazione del sigillo;
- stati rituali dell'orb;
- logica Chorus;
- calcolo afterglow;
- regole di privacy e celle anonime;
- repository astratti.

Da costruire nativo iOS:

- UI SwiftUI;
- renderer orb con Canvas/Metal o SwiftUI Canvas;
- CoreMotion per movimento e shake;
- haptics iOS;
- notifiche locali;
- integrazione Firebase iOS;
- storage locale;
- permessi posizione approssimativa;
- TestFlight.

Prerequisiti:

- Mac con Xcode;
- Apple Developer account;
- iPhone reale per test sensori/haptics;
- definizione bundle ID iOS;
- configurazione Firebase iOS nello stesso progetto.

## Test Finale Con Due Telefoni

Da lasciare alla fine, quando:

- Nearby Field e stabile;
- Echo visuale e completo;
- Chorus live usa Firebase;
- afterglow viene salvato;
- fallback offline e chiaro.

Scenario:

1. Installare la stessa build su due telefoni.
2. Sigillare il giorno su entrambi.
3. Aprire Nearby Field nella stessa area.
4. Verificare presenza reciproca anonima.
5. Inviare echo da telefono A a telefono B.
6. Verificare cicatrice/trace su entrambi.
7. Entrare nel Chorus con entrambi.
8. Verificare convergenza live e afterglow.

## Principi Da Non Tradire

- Niente profili.
- Niente feed.
- Niente chat.
- Niente classifica.
- Niente performance.
- Niente spiegazioni eccessive in app.
- Il gesto deve pesare piu dell'interfaccia.
- Il silenzio e parte del prodotto.
- Il Chorus deve sembrare raro, inevitabile, condiviso.
