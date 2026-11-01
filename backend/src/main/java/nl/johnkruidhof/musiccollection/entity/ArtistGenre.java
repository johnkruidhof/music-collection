package nl.johnkruidhof.musiccollection.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bij deze gebruiker hoort artiest {@link #artistKey} bij {@link #genre} (F-28): voorstel bij toevoegen. */
@Entity
@Table(name = "artist_genre")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistGenre {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Genormaliseerd met {@code Artists.key}. */
    @Column(name = "artist_key", nullable = false, updatable = false, length = 500)
    private String artistKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    public ArtistGenre(UUID userId, String artistKey, Genre genre) {
        this.userId = userId;
        this.artistKey = artistKey;
        this.genre = genre;
    }
}
