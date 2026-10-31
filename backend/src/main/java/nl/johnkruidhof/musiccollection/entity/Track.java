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

@Entity
@Table(name = "track")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @Column(name = "disc_number", nullable = false)
    private int discNumber = 1;

    /** Positie zoals op de hoes, bijv. "A1", "B2" of "3". */
    private String position;

    /** Numerieke volgorde, omdat {@link #position} niet altijd goed sorteert. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private String title;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /** Alleen gevuld als de track-artiest afwijkt van de release-artiest (compilaties). */
    private String artist;

    public Track(int discNumber, int sortOrder, String position, String title) {
        this.discNumber = discNumber;
        this.sortOrder = sortOrder;
        this.position = position;
        this.title = title;
    }
}
