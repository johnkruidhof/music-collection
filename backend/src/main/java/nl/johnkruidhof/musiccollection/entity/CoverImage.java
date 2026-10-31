package nl.johnkruidhof.musiccollection.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Een door de gebruiker geüploade cover (alleen voor handmatige releases), opgeslagen in de database. */
@Entity
@Table(name = "cover_image")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoverImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "release_id", nullable = false, unique = true, updatable = false)
    private UUID releaseId;

    @Column(nullable = false)
    private byte[] image;

    @Column(nullable = false)
    private byte[] thumbnail;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CoverImage(UUID releaseId, byte[] image, byte[] thumbnail, String contentType) {
        this.releaseId = releaseId;
        this.image = image;
        this.thumbnail = thumbnail;
        this.contentType = contentType;
    }
}
