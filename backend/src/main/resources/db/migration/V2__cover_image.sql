-- Eigen cover-uploads voor handmatig ingevoerde releases (F-17).
-- Eén cover per release; bij vervangen komt er een nieuwe rij (nieuw id = nieuwe URL, dus geen
-- verouderde browsercache).
CREATE TABLE cover_image (
    id            UUID PRIMARY KEY,
    release_id    UUID         NOT NULL UNIQUE REFERENCES release (id) ON DELETE CASCADE,
    image         BYTEA        NOT NULL,
    thumbnail     BYTEA        NOT NULL,
    content_type  VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
