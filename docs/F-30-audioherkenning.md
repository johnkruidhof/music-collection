# F-30 — Onderzoek audioherkenning ("luisteren")

Doel: een plaat/cd laten spelen, ~10 seconden opnemen met de microfoon van de telefoon of laptop, en
herkennen welk nummer het is. Daarna koppelen aan de catalogus (toevoegen / "staat al in je collectie")
en, in F-32, aan de eigen collectie ("welke van mijn platen is dit?").

## Opnemen in de browser

- `navigator.mediaDevices.getUserMedia({ audio: … })` + `MediaRecorder`; werkt alleen in een *secure
  context* (HTTPS via Caddy, of `localhost`).
- Uitvoer verschilt per browser: Chrome/Edge/Firefox `audio/webm;codecs=opus`, Safari `audio/mp4` (AAC).
  We sturen het bestand zoals het is door; een omzetting naar MP3 in de browser is niet nodig zolang de
  dienst beide formaten accepteert (**te verifiëren met een echte sleutel**, zie "Open punten").
- Echo-onderdrukking en ruisonderdrukking uitzetten (`echoCancellation: false`, `noiseSuppression: false`,
  `autoGainControl: false`): die zijn gemaakt voor spraak en vervormen muziek.
- 10 seconden is ruim voldoende voor fingerprinting en blijft ver onder de 10 MB (~150 KB Opus).

## Diensten

| Dienst | Geschikt voor microfoon | Kosten | Integratie |
|---|---|---|---|
| **AudD** (gekozen) | Ja (Shazam-achtig) | 300 gratis verzoeken (ca. 2 weken), daarna ca. $2–5 per 1000 | `POST https://api.audd.io/` met `api_token` + `file` (multipart), max. 10 MB, geen rate limit |
| ACRCloud | Ja, grootste database | 14 dagen proef; prijs alleen na inloggen | Access key + secret met HMAC-ondertekening |
| AcoustID / Chromaprint | Nee: bedoeld voor schone, volledige bestanden | Gratis | Fingerprint lokaal berekenen, dan opzoeken |

Keuze (met de gebruiker): **AudD** — eenvoudigste integratie, voorspelbare kosten per verzoek, genoeg voor
thuisgebruik. ACRCloud kan later achter dezelfde interface (`client.AudioRecognizer`).

## AudD in het kort

- Antwoord bij een match: `status: "success"`, `result: { artist, title, album, release_date, label,
  timecode, song_link }`; geen match: `result: null`.
- Fouten: `status: "error"`, `error: { error_code, error_message }`. Relevant: 901 (limiet bereikt /
  geen token), 900 (ongeldige token), 300 (fingerprint mislukt, vaak te korte/stille opname),
  500 (ongeldig audiobestand), 400 (te groot).
- Optioneel `return=musicbrainz,apple_music,spotify` voor extra metadata; nu niet gebruikt.

## Hoe het in de app werkt (F-30)

1. `POST /api/recognize` (multipart `file`) → `RecognitionService` → `AuddRecognizer` (buiten een
   databasetransactie; time-out 30 s).
2. Niets herkend → `{ track: null }`.
3. Herkend → `inCollection`: eigen items waarvan de artiest overeenkomt en een track of de releasetitel
   het nummer/album bevat (zonder toevoegingen als "(Remastered)", "- Live", "feat. …").
4. `candidates`: zoeken op artiest + album via de bestaande lookup (Discogs → MusicBrainz, gecachet), met
   `ownedCount`/`onWishlist`. Faalt die zoekactie, dan blijft het herkende nummer gewoon zichtbaar.
5. Zonder `AUDD_API_TOKEN` geeft de API 503 "Luisteren niet beschikbaar" en `GET /api/recognize/status`
   meldt `enabled: false`, zodat de frontend de functie kan verbergen.

## Open punten

- **Formaat verifiëren** met een echte sleutel: herkent AudD WebM/Opus uit Chrome en MP4/AAC uit Safari?
  De documentatie noemt MP3/AAC "en andere gangbare formaten". Zo niet: in de backend omzetten
  (ffmpeg in de container) of in de browser naar WAV coderen.
- **Herkenning van live-opnames/remasters**: AudD geeft het album van de herkende versie; dat kan een
  compilatie zijn. Daarom toont de app ook "staat al in je collectie" op basis van tracktitels.
- **Kosten**: elke druk op "Luisteren" kost een verzoek. Geen automatische herhaling.
