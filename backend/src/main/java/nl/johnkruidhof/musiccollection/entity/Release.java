package nl.johnkruidhof.musiccollection.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Een release in de gedeelde catalogus (één specifieke persing/uitgave).
 * Wordt gedeeld door alle gebruikers; persoonlijke gegevens staan in {@link CollectionItem}.
 */
@Entity
@Table(name = "release")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String barcode;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private String title;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "format_details")
    private String formatDetails;

    private String label;

    @Column(name = "catalog_number")
    private String catalogNumber;

    private String country;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "cover_thumbnail_url")
    private String coverThumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetadataSource source;

    @Column(name = "discogs_id")
    private String discogsId;

    @Column(name = "musicbrainz_id")
    private String musicbrainzId;

    @OneToMany(mappedBy = "release", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("discNumber ASC, sortOrder ASC")
    private List<Track> tracks = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Release(String artist, String title, MediaType mediaType, MetadataSource source) {
        this.artist = artist;
        this.title = title;
        this.mediaType = mediaType;
        this.source = source;
    }

    public void addTrack(Track track) {
        track.setRelease(this);
        tracks.add(track);
    }

    public void clearTracks() {
        tracks.forEach(t -> t.setRelease(null));
        tracks.clear();
    }
}
