-- F-28: genres per gebruiker (met optioneel subgenre), toegekend aan collectie-items en wensen.
-- path = genre (of "genre / subgenre") in kleine letters: uniek per gebruiker en sorteersleutel.
CREATE TABLE genre (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    parent_id   UUID         REFERENCES genre (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    path        VARCHAR(210) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_genre_user_path UNIQUE (user_id, path)
);

CREATE INDEX idx_genre_parent ON genre (parent_id);

-- Welk genre een artiest bij deze gebruiker heeft (voorstel bij toevoegen; uit de boomstructuur of geleerd).
-- artist_key = genormaliseerde artiestnaam ("Cinematic Orchestra, The" en "The Cinematic Orchestra" gelijk).
CREATE TABLE artist_genre (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    artist_key  VARCHAR(500) NOT NULL,
    genre_id    UUID         NOT NULL REFERENCES genre (id) ON DELETE CASCADE,
    CONSTRAINT uq_artist_genre_user_artist UNIQUE (user_id, artist_key)
);

ALTER TABLE collection_item ADD COLUMN genre_id UUID REFERENCES genre (id) ON DELETE SET NULL;
ALTER TABLE wishlist_item ADD COLUMN genre_id UUID REFERENCES genre (id) ON DELETE SET NULL;
CREATE INDEX idx_collection_item_genre ON collection_item (genre_id);
CREATE INDEX idx_wishlist_item_genre ON wishlist_item (genre_id);

-- Standaardgenres worden bij de eerste keer eenmalig aangemaakt; daarna niet opnieuw (ook niet als de
-- gebruiker ze allemaal verwijdert).
ALTER TABLE app_user ADD COLUMN genres_initialized BOOLEAN NOT NULL DEFAULT FALSE;
