-- F-21: covers van Discogs/MusicBrainz lokaal cachen.
-- cover_source_url bewaart de originele (externe) URL zodra de cover lokaal staat;
-- cover_cache_attempts telt mislukte downloads, zodat een kapotte URL niet eindeloos opnieuw wordt geprobeerd.
ALTER TABLE release ADD COLUMN cover_source_url VARCHAR(1000);
ALTER TABLE release ADD COLUMN cover_cache_attempts INTEGER NOT NULL DEFAULT 0;
