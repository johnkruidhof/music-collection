# Changelog

Versies volgen [Semantic Versioning](https://semver.org/lang/nl/). Tot 1.0.0 is de app functioneel in
ontwikkeling en nog niet ingericht voor publiek gebruik.

## [0.10.0] — 2026-11-01

Genres: de collectie ordenen op eigen genres en subgenres. Nog steeds een pre-release; 1.0.0 volgt na fase 4.

### Genres (#25, #26)
- **Eigen genres per gebruiker**, met optionele subgenres. Nieuwe gebruikers starten met een standaardlijst
  van 14 genres, waaronder Hardrock - Metal met 10 subgenres (AOR, Doom Metal, Grunge, …).
- **Boomstructuur importeren** (Collectie → Genres): een boomstructuur van je muziekmappen
  (*Genre / [Subgenre /] Artiest / 1997 - Album*) maakt ontbrekende genres aan, koppelt artiesten aan hun
  genre en deelt bestaande releases en wensen zonder genre direct in. Subgenres worden aan de structuur
  herkend; opnieuw importeren is veilig.
- **Genre-voorstel** op basis van de artiest bij toevoegen (scannen, zoeken, handmatig, verlanglijst,
  Discogs-import); een zelf gekozen genre wordt onthouden voor die artiest.
- **Collectie-overzicht**: filteren op genre (een hoofdgenre toont ook zijn subgenres, of "zonder genre")
  en sorteren op genre met een kopje per genre/subgenre.
- Genre op de detailpagina en bij handmatig toevoegen, per wens op de verlanglijst (met filter), een
  verdeling *Per genre* in de statistieken en de kolommen Genre en Subgenre in de CSV-export.
- Genres beheren: toevoegen, subgenres toevoegen, hernoemen, verwijderen (releases houden hun plek, zonder
  genre).

### Beveiliging
- Spring Boot 4.1.2: bijgewerkte Tomcat, Spring Framework, Jackson, Log4j en PostgreSQL-driver met
  oplossingen voor bekende kwetsbaarheden, waaronder kritieke in Tomcat en Spring (#27).
- `minimatch` 11 in de lint-tooling van de frontend (DoS-lek, alleen development) (#28).
- Dependabot alerts en security updates staan aan voor de repository.

## [0.9.0] — 2026-11-01

Eerste release: **functioneel compleet, nog niet productieklaar** (pre-release). Fase 0 t/m 3 van de backlog
in CLAUDE.md zijn af; fase 4 (publiek online) volgt richting 1.0.0.

### Raamwerk (fase 0)
- Monorepo met Spring Boot 4.1 (Java 25) en Angular 22, alles via Docker Compose; Maven Wrapper (#1),
  ESLint + Prettier (#2)
- Keycloak (eigen realm, self-registration, rollen `user`/`admin`), Postgres met Flyway, Caddy met HTTPS
- GitHub Actions: backend `verify`, frontend lint, Prettier-check, build en tests

### Collectie (fase 1)
- Discogs-client (#3) en MusicBrainz + Cover Art Archive als fallback (#4)
- Lookup-service: barcode-normalisatie, lokale catalogus eerst, caching, rate limiting en retry (#5)
- Collectie-API met paginering, filters en zoeken; alleen eigen items (#6)
- Barcode scannen met de camera, batch-modus (#7); zoeken op artiest/titel/jaar (#8); handmatig invoeren
  met tracks en eigen cover (#9); collectie-overzicht als grid of lijst (#10); detailpagina met tracklist
  en eigen velden (#11)

### Uitbreidingen (fase 2)
- Statistieken (#12); covers lokaal gecachet (#13); CSV-export en import van de Discogs-collectie (#14)
- Dubbelen-detectie (#15); verlanglijst (#16); installeerbare PWA met offline collectie (#17);
  catalogusbeheer voor beheerders (#18)

### Luisteren (fase 3)
- Muziekherkenning via AudD (#19), luisteren in de toevoeg-flow (#20), "Welke van mijn platen is dit?"
  (#21); templates en styles in aparte bestanden (#23)

### Opgelost
- Na inloggen via een diepe link kwam je op de startpagina uit (#12)
- Opslaan op de detailpagina herlaadde de pagina (#11)
- HTTPS werkte niet met een IP-adres als `APP_HOST` (#22)

### Bekende beperkingen
- **Niet productieklaar**: Keycloak draait in dev-modus met testgebruikers en ruime redirect-URI's; Postgres-
  en Keycloak-poorten staan open; lokale CA in plaats van Let's Encrypt. Dit wordt opgelost in fase 4 (F-40).
- Luisteren is nog niet getest met een echte AudD-sleutel; of AudD WebM/Opus uit Chrome accepteert is onzeker.
- De service worker (offline) werkt alleen als het root-certificaat van Caddy vertrouwd is.
- Importvoortgang staat in het geheugen en is na een herstart van de backend weg (opnieuw importeren is veilig).
- Nog geen dubbelen-controle bij handmatig invoeren en geen import van het eigen CSV-formaat.

[0.10.0]: https://github.com/johnkruidhof/music-collection/releases/tag/v0.10.0
[0.9.0]: https://github.com/johnkruidhof/music-collection/releases/tag/v0.9.0
