-- Gebruikers (gekoppeld aan Keycloak via de 'sub' claim)
CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    keycloak_id   VARCHAR(255) NOT NULL UNIQUE,
    username      VARCHAR(255) NOT NULL,
    email         VARCHAR(255),
    display_name  VARCHAR(255),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Gedeelde catalogus
CREATE TABLE release (
    id                   UUID PRIMARY KEY,
    barcode              VARCHAR(32),
    artist               VARCHAR(500) NOT NULL,
    title                VARCHAR(500) NOT NULL,
    release_year         INTEGER,
    media_type           VARCHAR(20)  NOT NULL
        CHECK (media_type IN ('VINYL', 'CD', 'DVD', 'BLU_RAY', 'CASSETTE', 'OTHER')),
    format_details       VARCHAR(255),
    label                VARCHAR(255),
    catalog_number       VARCHAR(100),
    country              VARCHAR(100),
    cover_image_url      VARCHAR(1000),
    cover_thumbnail_url  VARCHAR(1000),
    source               VARCHAR(20)  NOT NULL
        CHECK (source IN ('DISCOGS', 'MUSICBRAINZ', 'MANUAL')),
    discogs_id           VARCHAR(50)  UNIQUE,
    musicbrainz_id       VARCHAR(50)  UNIQUE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_release_barcode ON release (barcode);
CREATE INDEX idx_release_artist_title ON release (lower(artist), lower(title));

CREATE TABLE track (
    id                UUID PRIMARY KEY,
    release_id        UUID NOT NULL REFERENCES release (id) ON DELETE CASCADE,
    disc_number       INTEGER NOT NULL DEFAULT 1,
    position          VARCHAR(20),
    sort_order        INTEGER NOT NULL,
    title             VARCHAR(500) NOT NULL,
    duration_seconds  INTEGER,
    artist            VARCHAR(500)
);

CREATE INDEX idx_track_release ON track (release_id);

-- Persoonlijke collectie (meerdere exemplaren van dezelfde release zijn toegestaan)
CREATE TABLE collection_item (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    release_id         UUID NOT NULL REFERENCES release (id),
    condition_media    VARCHAR(20)
        CHECK (condition_media IN ('MINT', 'NEAR_MINT', 'VERY_GOOD_PLUS', 'VERY_GOOD', 'GOOD', 'POOR')),
    condition_sleeve   VARCHAR(20)
        CHECK (condition_sleeve IN ('MINT', 'NEAR_MINT', 'VERY_GOOD_PLUS', 'VERY_GOOD', 'GOOD', 'POOR')),
    purchase_date      DATE,
    purchase_price     NUMERIC(10, 2),
    purchase_location  VARCHAR(255),
    storage_location   VARCHAR(255),
    notes              VARCHAR(4000),
    added_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_collection_item_user ON collection_item (user_id);
CREATE INDEX idx_collection_item_release ON collection_item (release_id);
