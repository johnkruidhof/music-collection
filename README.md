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

## Scannen met je telefoon

De camera werkt in de browser alleen via HTTPS. Caddy maakt daarvoor een eigen, lokaal certificaat.

1. Zet in `.env` `APP_HOST` op het IP-adres van je computer (bijv. `192.168.1.50`) en herstart:
   `docker compose up -d`
2. Haal het root-certificaat van Caddy op:
   `docker compose cp caddy:/data/caddy/pki/authorities/local/root.crt ./caddy-root.crt`
3. Installeer `caddy-root.crt` als vertrouwd certificaat op je telefoon en computer:
   - **Android:** Instellingen → Beveiliging → Versleuteling en inloggegevens → Certificaat installeren → CA-certificaat
   - **iOS:** bestand openen → profiel installeren → Instellingen → Algemeen → Info → Certificaatvertrouwen → aanzetten
   - **macOS/Windows:** importeren in de systeem-sleutelhanger / certificaatbeheer als vertrouwde root
4. Open `https://<APP_HOST>` op je telefoon.

## Ontwikkelen (IntelliJ + ng serve)

```bash
# infrastructuur in Docker (APP_HOST=localhost)
docker compose up -d postgres keycloak caddy

# backend: open de map backend/ in IntelliJ als Maven-project en start MusicCollectionApplication
#          (profiel 'dev' is standaard actief), of:
cd backend && mvn spring-boot:run

# frontend
cd frontend && npm install && npm start      # http://localhost:4200
```

- Swagger UI (dev): http://localhost:8080/swagger-ui.html
- `mvn verify` draait ook integratietests met Testcontainers en heeft dus een draaiende Docker nodig.
