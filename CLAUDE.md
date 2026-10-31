# CLAUDE.md — Muziekcollectie (vinyl / cd / dvd / blu-ray)

Dit bestand is de leidraad voor Claude Code (CLI) bij het verder ontwikkelen van dit project.
Lees dit bestand volledig voordat je aan een feature begint, en werk de **Feature backlog** bij
(checkbox afvinken + korte notitie) wanneer een feature af is.

---

## 1. Doel

Een multi-user webapplicatie om een fysieke **muziek**collectie bij te houden.

- Een medium toevoegen door de **barcode te scannen** (ook met de telefoon); de backend haalt
  automatisch artiest, titel, tracks, jaartal, cover en type medium op uit centrale muziekdatabases.
- Zonder barcode: **zoeken op artiest / titel / jaartal** en het juiste resultaat kiezen, of volledig
  **handmatig** invoeren.
- Elke gebruiker heeft een eigen, privé collectie. Gebruikers kunnen **zichzelf registreren** (Keycloak).
- Later: **luisteren** naar muziek en op basis daarvan titels herkennen.

Bewuste keuzes:
- **Alleen muziek.** Dvd's en blu-rays zijn concertregistraties, muziekdocumentaires e.d.; films
  vallen buiten scope. Daarom volstaan Discogs en MusicBrainz als bronnen.
- **Geen delen** van collecties tussen gebruikers.
- Draait eerst **thuis/lokaal**, gaat later mogelijk **publiek online** (zie F-40 e.v.).

---

## 2. Tech stack

| Laag         | Keuze                                                                  |
|--------------|------------------------------------------------------------------------|
| Backend      | Java 25, Spring Boot 4.1, Maven                                        |
| Persistentie | Spring Data JPA (Hibernate 7), PostgreSQL 17, Flyway                   |
| Security     | Spring Security OAuth2 Resource Server (JWT), Keycloak 26.7            |
| Codegeneratie| Lombok, MapStruct                                                      |
| HTTP-clients | Spring `RestClient` met `@HttpExchange` interfaces                     |
| Frontend     | Angular 22 (standalone, zoneless, signals), Angular Material           |
| Frontend-auth| `angular-oauth2-oidc` (Authorization Code + PKCE)                      |
| Barcode      | `@zxing/browser` (camera via `getUserMedia`), handmatige invoer als fallback |
| HTTPS        | Caddy als reverse proxy met lokale CA (`local_certs`)                  |
| Tests        | JUnit 5, Mockito, Testcontainers 2 (Postgres), WireMock; Vitest (frontend) |
| Runtime      | Docker Compose (alles met één commando)                                |
| VCS / CI     | Git, GitHub, GitHub Actions                                            |

Spring Boot 4 let op: starters heten o.a. `spring-boot-starter-webmvc`,
`spring-boot-starter-security-oauth2-resource-server`, `spring-boot-starter-flyway`,
`spring-boot-starter-restclient`; Jackson 3 (`tools.jackson`), test-annotaties in
`org.springframework.boot.<module>.test.autoconfigure`, `@MockitoBean` i.p.v. `@MockBean`.

---

## 3. Repository-structuur (monorepo)

```
music-collection/
├── CLAUDE.md
├── README.md                    # starten, telefoon-HTTPS, ontwikkelen
├── docker-compose.yml
├── .env.example                 # alle secrets/tokens; .env staat in .gitignore
├── .github/workflows/ci.yml
├── docker/
│   ├── postgres/init/01-create-databases.sh   # db's + users 'musiccollection' en 'keycloak'
│   ├── keycloak/realm-music.json              # realm, clients, rollen, testgebruikers
│   └── caddy/Caddyfile                        # HTTPS + routing
├── backend/                     # Maven-project — in IntelliJ openen als Maven-project
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/...
└── frontend/                    # Angular-project
    ├── package.json
    ├── proxy.conf.json          # ng serve: /api -> localhost:8080
    ├── Dockerfile               # build + nginx
    ├── nginx.conf
    └── src/...
```

---

## 4. Backend-architectuur

### 4.1 Package-structuur

Base package / groupId: **`nl.johnkruidhof.musiccollection`** / `nl.johnkruidhof`.

```
nl.johnkruidhof.musiccollection
├── MusicCollectionApplication.java
├── config/          # ALLE configuratie: SecurityConfig, KeycloakJwtAuthenticationConverter, CorsConfig,
│                    # JpaAuditingConfig, CacheConfig, OpenApiConfig, AppProperties (+ PropertiesConfig)
├── controller/      # @RestController's — alleen HTTP-mapping, validatie, DTO in/uit
├── service/         # businesslogica, transacties (@Transactional hier)
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities + enums
├── dto/             # ALLE DTO's (Java records), sub-packages per domein:
│   ├── collection/
│   ├── release/
│   ├── lookup/
│   └── user/
├── mapper/          # entity <-> dto mapping (MapStruct, componentModel=spring)
├── exception/       # ALLE exceptions + GlobalExceptionHandler (@RestControllerAdvice)
└── client/          # externe API-clients achter MetadataProvider
    ├── discogs/
    └── musicbrainz/
```

### 4.2 Regels

- Verkeer loopt **altijd** `Controller -> Service -> Repository`. Controllers praten nooit direct
  met repositories of externe clients.
- Controllers ontvangen en retourneren **alleen DTO's**, nooit entities.
- DTO's zijn Java `record`s met Bean Validation annotaties (geen Lombok in DTO's).
- **Lombok** in entities, services en controllers: `@Getter`/`@Setter`, `@RequiredArgsConstructor`
  voor constructor injection, `@Slf4j`. **Geen** `@Data`, `@EqualsAndHashCode` of `@ToString` op
  entities (lazy loading / recursie). Entities krijgen `@NoArgsConstructor(access = PROTECTED)`.
- Mapping uitsluitend via **MapStruct**-mappers in `mapper/`; `unmappedTargetPolicy=ERROR` staat aan,
  dus elk doelveld moet bewust gemapt of `ignore`d worden.
- Configuratie uitsluitend in `config/`; waarden via `application.yml` + env-vars, nooit
  hardcoded secrets. Nieuwe instellingen onder `app.*` in `AppProperties`.
- Exceptions: domeinspecifieke exceptions in `exception/`; `GlobalExceptionHandler` vertaalt naar
  RFC 9457 `ProblemDetail`.
- Schema wordt beheerd door **Flyway** (`ddl-auto: validate`). Entity en migratie moeten exact
  overeenkomen (de context-test met Testcontainers controleert dit).
- Elke gebruiker ziet en wijzigt uitsluitend zijn eigen collectie-items: queries altijd scopen op
  `userId` (`findByIdAndUserId`), eigenaar via `UserService.getOrCreate(jwt)`. Een item van een
  ander geeft 404, niet 403.

### 4.3 Datamodel

Gedeelde **catalogus** (`release`) gescheiden van de persoonlijke **collectie** (`collection_item`).
Zie `V1__initial_schema.sql`.

```
app_user         id, keycloak_id (= JWT sub, uniek), username, email, display_name, created_at
release          id, barcode, artist, title, release_year, media_type, format_details, label,
                 catalog_number, country, cover_image_url, cover_thumbnail_url, source,
                 discogs_id (uniek), musicbrainz_id (uniek), created_at, updated_at
track            id, release_id, disc_number, position ("A1"), sort_order, title, duration_seconds, artist
collection_item  id, user_id, release_id, condition_media, condition_sleeve, purchase_date,
                 purchase_price, purchase_location, storage_location, notes, added_at, updated_at
```

Enums: `MediaType` (VINYL, CD, DVD, BLU_RAY, CASSETTE, OTHER), `MetadataSource` (DISCOGS,
MUSICBRAINZ, MANUAL), `MediaCondition` (Goldmine: MINT … POOR). Meerdere exemplaren van dezelfde
release per gebruiker zijn toegestaan.

### 4.4 Externe databronnen

| Bron | Gebruik | Auth / limieten |
|------|---------|-----------------|
| **Discogs API** | Primaire bron: barcode, zoeken, formaat, tracks, cover | Personal access token (`DISCOGS_TOKEN`), verplichte `User-Agent`, 60 req/min |
| **MusicBrainz API** | Fallback / aanvulling | Geen token, verplichte `User-Agent`, max 1 req/s |
| **Cover Art Archive** | Covers bij MusicBrainz-releases | Geen token |

Strategie lookup (F-12):
1. Barcode normaliseren (spaties/streepjes weg, UPC-A ↔ EAN-13 met voorloopnul).
2. Eerst lokaal in `release` zoeken (al eerder opgehaald → direct terug, geen API-call).
3. Anders Discogs; geen resultaat → MusicBrainz.
4. Meerdere kandidaten (persingen) → lijst teruggeven, gebruiker kiest.
5. Cachen met Caffeine (caches staan in `CacheConfig` / `application.yml`).
6. Rate limiting en retry per client.

Alle bronnen implementeren `client.MetadataProvider`.

### 4.5 REST API

```
GET    /api/me                                   huidige gebruiker (aangemaakt bij eerste aanroep)   ✔

GET    /api/lookup/barcode/{barcode}             kandidaten uit lokale catalogus + externe bronnen
GET    /api/lookup/search?artist=&title=&year=&mediaType=   zoeken zonder barcode
GET    /api/lookup/{source}/{externalId}         volledige details (incl. tracks) van één kandidaat

GET    /api/collection?q=&mediaType=&sort=&page=&size=   eigen collectie (gepagineerd)
POST   /api/collection                           toevoegen: { source, externalId } óf handmatige data
GET    /api/collection/{id}
PUT    /api/collection/{id}                      eigen velden wijzigen (staat, locatie, notities…)
DELETE /api/collection/{id}

GET    /api/collection/stats                     aantallen per type, per decennium, …
```

Swagger UI in het dev-profiel: http://localhost:8080/swagger-ui.html

---

## 5. Security (Keycloak)

- Realm `music`, geïmporteerd bij de eerste start uit `docker/keycloak/realm-music.json`
  (bestaat de realm al, dan wordt de import overgeslagen → wijzigingen via admin console of
  volume/DB wissen).
- Keycloak draait achter Caddy op **`https://<APP_HOST>/auth`** (`KC_HTTP_RELATIVE_PATH=/auth`).
  Admin console: `https://<APP_HOST>/auth/admin`.
- Clients:
  - `music-frontend` — public client, Authorization Code + PKCE (S256), audience-mapper die
    `music-backend` in de `aud` van het access token zet.
  - `music-backend` — bearer-only.
- Rollen: `user` (standaard via `default-roles-music`, ook voor zelf geregistreerde gebruikers),
  `admin`. Backend mapt `realm_access.roles` naar `ROLE_*` (`KeycloakJwtAuthenticationConverter`).
- Self-registration staat aan; frontend opent het registratiescherm met `prompt=create`.
  E-mailverificatie en wachtwoord-reset staan uit (geen SMTP) → F-42.
- Backend: `issuer-uri` = publieke URL (moet gelijk zijn aan `iss`), `jwk-set-uri` = interne URL
  (`http://keycloak:8080/...` in Docker, `http://localhost:8180/...` vanuit IntelliJ),
  `audiences: music-backend`.
- Testgebruikers (alleen dev): `alice/alice` (admin), `bob/bob`.
- **Dev-instellingen die vóór publiek gaan dicht moeten** (F-40): redirect-URI `https://*`,
  webOrigins `*`, testgebruikers, `start-dev`.

---

## 6. Frontend (Angular)

```
src/app/
├── core/
│   ├── auth/      # auth.config, AuthService (signals), authGuard, authInterceptor
│   ├── api/       # één service per backend-resource (UserApiService, ...)
│   └── models/    # TypeScript-interfaces die de backend-DTO's spiegelen
├── shared/        # herbruikbare components/pipes
└── features/      # lazy geladen pagina's
    ├── home/
    ├── collection/    # overzicht + detail
    └── add/           # scan-flow, zoek-flow, handmatig formulier
```

Routes: `/` (home), `/collectie`, `/toevoegen` — de laatste twee achter `authGuard`.
De OIDC-issuer wordt afgeleid van `window.location.origin` (`/auth/realms/music`), zodat één build
werkt op localhost, een LAN-IP en later een domein; `environment.development.ts` overschrijft dit
voor `ng serve`.

**Snelle-toevoeg-flow (scannen, F-14):**
1. Knop "Scannen" opent de achtercamera met `@zxing/browser` (EAN-13, UPC-A).
2. Barcode herkend → trillen/geluid → `GET /api/lookup/barcode/{code}`.
3. Eén kandidaat → bevestigingskaart (cover, artiest, titel, jaar, type) → één tik "Toevoegen".
4. Meerdere kandidaten → keuzelijst. Geen resultaat → zoeken/handmatig met barcode ingevuld.
5. Na toevoegen direct terug naar de scanner (batch-modus).

Camera en microfoon werken alleen in een *secure context* → altijd via Caddy (HTTPS) testen op de
telefoon. Zie README voor het installeren van het Caddy root-certificaat.

---

## 7. Docker

`docker compose up -d --build` start alles; de app is bereikbaar op `https://<APP_HOST>`.

| Service  | Poort (host)  | Opmerking |
|----------|---------------|-----------|
| caddy    | 80, 443       | HTTPS-ingang: `/api` → backend, `/auth` → keycloak, rest → frontend |
| postgres | 5432          | databases `musiccollection` + `keycloak` |
| keycloak | 8180 → 8080   | `start-dev --import-realm`, health op management-poort 9000 |
| backend  | (intern 8080) | profiel `prod`, wacht op healthy Postgres + Keycloak |
| frontend | (intern 80)   | nginx met de Angular-build |

Lokaal ontwikkelen: `docker compose up -d postgres keycloak caddy` met `APP_HOST=localhost`,
backend vanuit IntelliJ (profiel `dev`), frontend met `npm start` op http://localhost:4200.

---

## 8. Commando's

```bash
# alles starten / stoppen
docker compose up -d --build
docker compose down            # -v erbij wist ook database en certificaten

# backend (map backend/)
./mvnw spring-boot:run          # profiel 'dev'
./mvnw verify                   # unit + integratietests (Testcontainers → Docker nodig)

# frontend (map frontend/)
npm install
npm start                      # ng serve met proxy naar :8080
npm run build
npx ng test --watch=false
```

---

## 9. Werkafspraken voor Claude Code

- Werk feature voor feature uit de backlog; één feature = één branch
  (`feature/F-xx-korte-naam`) = één PR naar `main`.
- Commit-berichten volgens Conventional Commits (`feat:`, `fix:`, `chore:`, …).
- Nieuwe databasewijzigingen alleen via een nieuwe Flyway-migratie (`V{n}__omschrijving.sql`);
  bestaande migraties nooit aanpassen.
- Externe API's in tests altijd mocken met WireMock; nooit echte calls in tests.
- Bij elke service: unit tests. Bij elke controller: `@WebMvcTest` met
  `@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})`,
  `@MockitoBean JwtDecoder` en `jwt()` uit spring-security-test (zie `UserControllerTest`).
- Houd de package-regels uit §4.2 strikt aan.
- UI-teksten in het Nederlands.
- Update deze CLAUDE.md wanneer architectuurkeuzes wijzigen.

---

## 10. Feature backlog

### Fase 0 — Raamwerk
- [x] **F-01** Monorepo-structuur, `.gitignore`, `README.md`, `.env.example`
  — GitHub-repo aangemaakt en gepusht (remote via SSH).
- [x] **F-02** Backend skeleton: Maven, Java 25, Spring Boot 4.1, package-structuur uit §4.1
  — compileert; `./mvnw verify` en CI zijn groen.
- [x] **F-03** Docker Compose: Postgres, Keycloak (realm-import), backend, frontend, Caddy
- [x] **F-04** Keycloak realm `music` met clients, rollen, self-registration, testgebruikers
- [x] **F-05** Backend security: resource server, JWT + audience, rolmapping, `GET /api/me`
- [x] **F-06** Angular skeleton: routing, Material, login/registratie via Keycloak (PKCE), interceptor, guard
- [x] **F-07** Flyway + eerste migratie, entities, repositories
- [x] **F-08** GlobalExceptionHandler met ProblemDetail
- [x] **F-09** GitHub Actions CI: backend `verify`, frontend build + test
- [x] **F-15** HTTPS via Caddy met lokale CA, zodat scannen met de telefoon werkt
- [x] **F-0a** Maven Wrapper toevoegen (`mvn wrapper:wrapper`) en Dockerfile/CI/README op `./mvnw` zetten
  — wrapper 3.3.4 (only-script) met Maven 3.9.16; Dockerfile bouwt op `eclipse-temurin:25-jdk`;
  `.gitattributes` houdt `mvnw` op LF.
- [ ] **F-0b** ESLint (`ng add angular-eslint`) + Prettier-config en `npm run lint` in CI

### Fase 1 — MVP collectiebeheer
- [ ] **F-10** Discogs-client (barcode + zoeken + release-details), WireMock-tests
- [ ] **F-11** MusicBrainz + Cover Art Archive client als fallback
- [ ] **F-12** Lookup-service met normalisatie, lokale catalogus-check, caching, rate limiting + lookup-controller
- [ ] **F-13** Collection CRUD-API (alleen eigen items), paginering, filters en zoeken
- [ ] **F-14** Frontend: scan-flow met camera + batch-modus
- [ ] **F-16** Frontend: zoeken op artiest/titel/jaar + kandidaat kiezen
- [ ] **F-17** Frontend: handmatig toevoegen/bewerken (incl. tracks en eigen cover-upload)
- [ ] **F-18** Frontend: collectie-overzicht (grid met covers / lijst), filters op type, zoeken, sorteren
- [ ] **F-19** Frontend: detailpagina met tracklist en eigen velden (staat, locatie, notities)

### Fase 2 — Uitbreidingen
- [ ] **F-20** Statistieken (per type, decennium, artiest, totale aankoopwaarde)
- [ ] **F-21** Covers lokaal opslaan/cachen i.p.v. hotlinken
- [ ] **F-22** Import/export (CSV, Discogs collection export importeren)
- [ ] **F-23** Dubbelen-detectie bij toevoegen ("je hebt deze al")
- [ ] **F-24** Wishlist naast collectie
- [ ] **F-26** PWA: installeerbaar op telefoon, offline collectie bekijken
- [ ] **F-27** Admin-scherm: catalogus opschonen, releases samenvoegen

### Fase 3 — Audio-herkenning ("luisteren")
- [ ] **F-30** Onderzoek & proof of concept audioherkenning
  - In de browser haalbaar: `getUserMedia` + `MediaRecorder` neemt ~10 s op via de microfoon
    (HTTPS vereist — via Caddy al geregeld).
  - Backend stuurt het fragment naar een herkenningsdienst:
    - **ACRCloud** of **AudD** — herkennen microfoonfragmenten (Shazam-achtig), betaald met gratis tier.
    - **AcoustID/Chromaprint** — gratis, maar bedoeld voor schone/volledige audiobestanden.
  - Resultaat (artiest/titel/ISRC) → match op MusicBrainz/Discogs → releases tonen
    → "Toevoegen aan collectie" of "Staat al in je collectie".
- [ ] **F-31** Frontend: knop "Luister" in de toevoeg-flow
- [ ] **F-32** "Welke van mijn platen is dit?"-modus: herkenning beperken tot eigen collectie

### Fase 4 — Publiek online (samen met de gebruiker uitwerken)
- [ ] **F-40** Hardening: Keycloak `start` (productie) met echte hostname, redirect-URI's en webOrigins
  beperken tot het domein, testgebruikers weg, poorten 5432/8180 niet meer publiceren, Caddy met
  Let's Encrypt (`local_certs` weg), sterke wachtwoorden/secrets
- [ ] **F-41** Hosting kiezen (VPS / thuis met port forwarding), back-ups van Postgres
- [ ] **F-42** SMTP in Keycloak: e-mailverificatie bij registratie en wachtwoord vergeten
- [ ] **F-43** Rate limiting / bescherming van de API, monitoring en logging

---

## 11. Gemaakte beslissingen

- Base package `nl.johnkruidhof.musiccollection`, groupId `nl.johnkruidhof`.
- Alleen muziek; dvd/blu-ray = muziekreleases (concerten e.d.).
- Discogs primair (gebruiker heeft account + token), MusicBrainz als fallback.
- Scannen met de telefoon → HTTPS via Caddy vanaf het begin.
- Self-registration aan; geen delen van collecties.
- Eerst thuis/lokaal, later publiek (fase 4, met begeleiding).
- Flyway, MapStruct, Angular Material, Lombok.
