# Muziekcollectie

Houd je vinyl, cd's, dvd's en blu-rays bij. Scan een barcode en artiest, titel, tracks, jaartal en
cover worden automatisch opgehaald (Discogs / MusicBrainz).

**Stack:** Java 25 · Spring Boot 4.1 · PostgreSQL · Keycloak · Angular 22 · Caddy (HTTPS) · Docker Compose

Zie [CLAUDE.md](CLAUDE.md) voor architectuur, afspraken en de feature backlog.

## Snel starten (alles in Docker)

```bash
cp .env.example .env        # wachtwoorden en evt. DISCOGS_TOKEN invullen
docker compose up -d --build
```

Open daarna **https://localhost** (of `https://<APP_HOST>`).

| Wat                    | URL                                   |
|------------------------|---------------------------------------|
| App                    | https://localhost                     |
| Keycloak admin console | https://localhost/auth/admin          |
| Testgebruikers         | `alice` / `alice` (admin), `bob` / `bob` |

Nieuwe gebruikers kunnen zichzelf registreren via **Account aanmaken**.

## Wat draait waar

Wachtwoorden staan nooit in de repo: ze komen uit `.env` (zie `.env.example`). Hieronder staat per onderdeel
de variabele waarin het wachtwoord staat.

| Onderdeel | Adres | Inloggen |
|---|---|---|
| **App** | `https://<APP_HOST>` | eigen account, of de testgebruikers `alice` (admin) / `bob` (alleen dev) |
| **Keycloak-beheer** | `https://<APP_HOST>/auth/admin` | `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` (realm *master*; de app gebruikt realm *music*) |
| **Database** (Postgres 17) | `localhost:5432`, database `musiccollection` | gebruiker `music` / `APP_DB_PASSWORD` |
| Database (beheerder) | `localhost:5432` | gebruiker `postgres` / `POSTGRES_PASSWORD` |
| **Keycloak direct** | `http://localhost:8180/auth` | alleen voor de backend vanuit IntelliJ (JWKS-sleutels) |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | alleen als je de backend vanuit IntelliJ start (profiel `dev`) |
| Frontend met `ng serve` | `http://localhost:4200` | zie *Ontwikkelen* hieronder |

### Database bekijken in IntelliJ

Database-paneel → **+** → **Data Source** → **PostgreSQL**, en vul in: host `localhost`, poort `5432`,
database `musiccollection`, gebruiker `music`, wachtwoord = `APP_DB_PASSWORD` uit `.env`
(URL: `jdbc:postgresql://localhost:5432/musiccollection`). De tabellen staan onder *schemas → public*.

Het schema wordt beheerd door Flyway (`backend/src/main/resources/db/migration`) en bij het starten van de
backend gecontroleerd: kijken en query's draaien kan veilig, maar wijzig geen tabellen met de hand. Zet bij
twijfel **Read-only** aan in de datasource-opties.

**Waar komt dit vandaan?** Poorten in `docker-compose.yml`, database en gebruikers in
`docker/postgres/init/01-create-databases.sh` (draait alleen bij de allereerste start), wachtwoorden in
`.env`, de verbinding van de backend in `backend/src/main/resources/application.yml`.

## Scannen met je telefoon

De camera werkt in de browser alleen via HTTPS. Caddy maakt daarvoor een eigen, lokaal certificaat.

1. Zet in `.env` `APP_HOST` op het IP-adres van je computer (bijv. `192.168.1.50`) en herstart
   **alle** services: `docker compose up -d` (zonder servicenamen). Caddy, Keycloak en de backend
   moeten hetzelfde adres gebruiken; open de app daarna via `https://<APP_HOST>`, ook op je computer
   (`https://localhost` werkt dan niet meer).
2. Haal het root-certificaat van Caddy op:
   `docker compose cp caddy:/data/caddy/pki/authorities/local/root.crt ./caddy-root.crt`
3. Installeer `caddy-root.crt` als vertrouwd certificaat op je telefoon en computer:
   - **Android:** Instellingen → Beveiliging → Versleuteling en inloggegevens → Certificaat installeren → CA-certificaat
   - **iOS:** bestand openen → profiel installeren → Instellingen → Algemeen → Info → Certificaatvertrouwen → aanzetten
   - **macOS/Windows:** importeren in de systeem-sleutelhanger / certificaatbeheer als vertrouwde root
4. Open `https://<APP_HOST>` op je telefoon.

## Als app installeren (PWA)

De muziekcollectie is een Progressive Web App: kies in de browser **Installeren** (Chrome/Edge) of
**Deel → Zet op beginscherm** (Safari). De app opent dan zonder adresbalk, met snelkoppelingen naar
*Scannen* en *Verlanglijst*.

- **Offline:** collectie, details, verlanglijst en statistieken die je eerder hebt bekeken blijven
  zichtbaar zonder verbinding (inclusief covers). Toevoegen en wijzigen kan alleen online.
- **Updates:** bij een nieuwe versie verschijnt een melding met *Vernieuwen*.
- De service worker werkt alleen met een **vertrouwd** certificaat: installeer eerst het root-certificaat
  van Caddy (zie hierboven), ook op je computer. Zonder dat werkt de site wel, maar niet offline.
- Uitloggen wist de offline bewaarde gegevens van dit apparaat.

## Ontwikkelen (IntelliJ + ng serve)

```bash
# infrastructuur in Docker (APP_HOST=localhost)
docker compose up -d postgres keycloak caddy

# backend: open de map backend/ in IntelliJ als Maven-project en start MusicCollectionApplication
#          (profiel 'dev' is standaard actief), of:
cd backend && ./mvnw spring-boot:run

# frontend
cd frontend && npm install && npm start      # http://localhost:4200
```

- Swagger UI (dev): http://localhost:8080/swagger-ui.html
- Frontend: `npm run lint` (ESLint) en `npm run format` (Prettier); Node ≥ 24.15 vereist.
- `./mvnw verify` draait ook integratietests met Testcontainers en heeft dus een draaiende Docker nodig.
