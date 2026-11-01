package nl.johnkruidhof.musiccollection.entity;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

/** Een genre (of subgenre, met {@link #parent}) van één gebruiker (F-28). */
@Entity
@Table(name = "genre")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Genre {

    public static final String PATH_SEPARATOR = " / ";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** {@code null} = hoofdgenre. Eén niveau diep: een subgenre heeft zelf geen subgenres. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", updatable = false)
    private Genre parent;

    @Column(nullable = false, length = 100)
    private String name;

    /** "genre" of "genre / subgenre" in kleine letters: uniek per gebruiker en de sorteervolgorde. */
    @Column(nullable = false, length = 210)
    private String path;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Genre(UUID userId, Genre parent, String name) {
        this.userId = userId;
        this.parent = parent;
        rename(name);
    }

    /** Wijzigt naam en path; subgenres van een hoofdgenre moeten daarna ook {@link #refreshPath()} krijgen. */
    public void rename(String newName) {
        this.name = newName.strip();
        refreshPath();
    }

    public void refreshPath() {
        String own = name.toLowerCase(Locale.ROOT);
        this.path = parent == null ? own : parent.getPath() + PATH_SEPARATOR + own;
    }

    public boolean isSubgenre() {
        return parent != null;
    }

    /** Het hoofdgenre: zichzelf, of de parent bij een subgenre. */
    public Genre topLevel() {
        return parent == null ? this : parent;
    }
}
