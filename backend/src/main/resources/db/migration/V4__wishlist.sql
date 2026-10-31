-- F-24: verlanglijst naast de collectie. Eén wens per release per gebruiker.
CREATE TABLE wishlist_item (
    id          UUID PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    release_id  UUID          NOT NULL REFERENCES release (id),
    notes       VARCHAR(1000),
    added_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_wishlist_user_release UNIQUE (user_id, release_id)
);

CREATE INDEX idx_wishlist_item_release ON wishlist_item (release_id);
