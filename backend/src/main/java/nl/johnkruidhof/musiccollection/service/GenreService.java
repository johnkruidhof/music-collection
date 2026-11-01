package nl.johnkruidhof.musiccollection.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.dto.genre.GenreDto;
import nl.johnkruidhof.musiccollection.dto.genre.GenreImportResultDto;
import nl.johnkruidhof.musiccollection.dto.genre.GenreRequest;
import nl.johnkruidhof.musiccollection.entity.AppUser;
import nl.johnkruidhof.musiccollection.entity.ArtistGenre;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;
import nl.johnkruidhof.musiccollection.entity.Genre;
import nl.johnkruidhof.musiccollection.entity.WishlistItem;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.repository.ArtistGenreRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.GenreRepository;
import nl.johnkruidhof.musiccollection.repository.WishlistItemRepository;

/**
 * Genres per gebruiker (F-28): beheer, standaardlijst, genre-voorstel op basis van de artiest (geleerd of
 * uit een geïmporteerde boomstructuur) en het importeren van zo'n boomstructuur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {

    static final String DEFAULT_GENRES = "/genres/standaard-genres.txt";
    static final int MAX_TREE_BYTES = 2 * 1024 * 1024;

    private final GenreRepository genreRepository;
    private final ArtistGenreRepository artistGenreRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final UserService userService;

    @Transactional
    public List<GenreDto> list(Jwt jwt) {
        AppUser user = userService.getOrCreate(jwt);
        ensureDefaults(user);
        List<Genre> genres = genreRepository.findByUserIdOrderByPath(user.getId());
        Map<UUID, Long> counts = new HashMap<>();
        genreRepository.countItemsPerGenre(user.getId()).forEach(c -> counts.put(c.getGenreId(), c.getCount()));
        List<GenreDto> result = new ArrayList<>();
        for (Genre top : genres.stream().filter(g -> !g.isSubgenre()).toList()) {
            List<GenreDto> subs = genres.stream()
                    .filter(g -> g.isSubgenre() && g.getParent().getId().equals(top.getId()))
                    .map(g -> new GenreDto(g.getId(), g.getName(), top.getId(), counts.getOrDefault(g.getId(), 0L),
                            List.of()))
                    .toList();
            result.add(new GenreDto(top.getId(), top.getName(), null, counts.getOrDefault(top.getId(), 0L), subs));
        }
        return result;
    }

    @Transactional
    public GenreDto create(Jwt jwt, GenreRequest request) {
        AppUser user = userService.getOrCreate(jwt);
        ensureDefaults(user);
        Genre parent = null;
        if (request.parentId() != null) {
            parent = resolve(user.getId(), request.parentId());
            if (parent.isSubgenre()) {
                throw new InvalidRequestException("Een subgenre kan zelf geen subgenres hebben.");
            }
        }
        Genre genre = new Genre(user.getId(), parent, request.name());
        assertUnique(user.getId(), genre.getPath(), null);
        Genre saved = genreRepository.save(genre);
        return new GenreDto(saved.getId(), saved.getName(), parent == null ? null : parent.getId(), 0, List.of());
    }

    @Transactional
    public GenreDto rename(Jwt jwt, UUID id, GenreRequest request) {
        AppUser user = userService.getOrCreate(jwt);
        Genre genre = resolve(user.getId(), id);
        genre.rename(request.name());
        assertUnique(user.getId(), genre.getPath(), genre.getId());
        if (!genre.isSubgenre()) {
            genreRepository.findByParentId(genre.getId()).forEach(Genre::refreshPath);
        }
        return new GenreDto(genre.getId(), genre.getName(), genre.isSubgenre() ? genre.getParent().getId() : null,
                0, List.of());
    }

    /** Subgenres gaan mee; items en wensen houden hun plek, maar zonder genre (FK: SET NULL). */
    @Transactional
    public void delete(Jwt jwt, UUID id) {
        AppUser user = userService.getOrCreate(jwt);
        genreRepository.delete(resolve(user.getId(), id));
        genreRepository.flush();
    }

    /** Een genre van deze gebruiker; anders 400 (een id van een ander verraadt niets). */
    public Genre resolve(UUID userId, UUID genreId) {
        return genreRepository.findByIdAndUserId(genreId, userId)
                .orElseThrow(() -> new InvalidRequestException("Onbekend genre."));
    }

    /** Genre-voorstel op basis van de artiest (uit de boomstructuur of eerder zelf gekozen). */
    public Optional<Genre> suggest(UUID userId, String artist) {
        String key = Artists.key(artist);
        return key.isEmpty() ? Optional.empty()
                : artistGenreRepository.findByUserIdAndArtistKey(userId, key).map(ArtistGenre::getGenre);
    }

    /** Onthoudt dat deze artiest bij dit genre hoort (bij handmatig kiezen), zodat het volgende keer voorgesteld wordt. */
    public void learn(UUID userId, String artist, Genre genre) {
        String key = Artists.key(artist);
        if (key.isEmpty() || genre == null) {
            return;
        }
        artistGenreRepository.findByUserIdAndArtistKey(userId, key).ifPresentOrElse(
                existing -> existing.setGenre(genre),
                () -> artistGenreRepository.save(new ArtistGenre(userId, key, genre)));
    }

    /**
     * Boomstructuur van een muziekmap importeren: ontbrekende genres/subgenres aanmaken, artiesten koppelen
     * (de boomstructuur wint van eerdere koppelingen) en items/wensen zónder genre indelen. Opnieuw
     * importeren is veilig.
     */
    @Transactional
    public GenreImportResultDto importTree(Jwt jwt, byte[] file) {
        if (file == null || file.length == 0) {
            throw new InvalidRequestException("Het bestand is leeg.");
        }
        if (file.length > MAX_TREE_BYTES) {
            throw new InvalidRequestException("Het bestand is te groot (max. 2 MB).");
        }
        List<GenreTree.ParsedGenre> parsed = GenreTree.parseTree(new String(file, StandardCharsets.UTF_8));
        if (parsed.isEmpty()) {
            throw new InvalidRequestException("Geen genres gevonden. Verwacht een boomstructuur met regels als \"|-- Rock\".");
        }
        AppUser user = userService.getOrCreate(jwt);
        ensureDefaults(user);
        UUID userId = user.getId();

        int[] created = new int[2];
        Map<String, Genre> artistToGenre = new HashMap<>();
        for (GenreTree.ParsedGenre g : parsed) {
            Genre top = getOrCreate(userId, null, g.name(), created, 0);
            g.artists().forEach(artist -> artistToGenre.putIfAbsent(Artists.key(artist), top));
            for (GenreTree.ParsedSubgenre s : g.subgenres()) {
                Genre sub = getOrCreate(userId, top, s.name(), created, 1);
                s.artists().forEach(artist -> artistToGenre.putIfAbsent(Artists.key(artist), sub));
            }
        }
        artistToGenre.remove("");

        Map<String, ArtistGenre> existing = new HashMap<>();
        artistGenreRepository.findByUserId(userId).forEach(a -> existing.put(a.getArtistKey(), a));
        int linked = 0;
        for (Map.Entry<String, Genre> entry : artistToGenre.entrySet()) {
            ArtistGenre current = existing.get(entry.getKey());
            if (current == null) {
                artistGenreRepository.save(new ArtistGenre(userId, entry.getKey(), entry.getValue()));
                linked++;
            } else if (!current.getGenre().getId().equals(entry.getValue().getId())) {
                current.setGenre(entry.getValue());
                linked++;
            }
        }

        int items = 0;
        for (CollectionItem item : collectionItemRepository.findByUserIdAndGenreIsNull(userId)) {
            Genre genre = artistToGenre.get(Artists.key(item.getRelease().getArtist()));
            if (genre != null) {
                item.setGenre(genre);
                items++;
            }
        }
        int wishes = 0;
        for (WishlistItem wish : wishlistItemRepository.findByUserIdAndGenreIsNull(userId)) {
            Genre genre = artistToGenre.get(Artists.key(wish.getRelease().getArtist()));
            if (genre != null) {
                wish.setGenre(genre);
                wishes++;
            }
        }
        log.info("Boomstructuur geïmporteerd: {} genres, {} subgenres nieuw, {} artiesten, {} items, {} wensen",
                created[0], created[1], linked, items, wishes);
        return new GenreImportResultDto(created[0], created[1], linked, items, wishes);
    }

    /** Eenmalig de standaardgenres aanmaken voor een gebruiker (ook als hij ze later allemaal verwijdert: niet opnieuw). */
    void ensureDefaults(AppUser user) {
        if (user.isGenresInitialized()) {
            return;
        }
        int[] created = new int[2];
        for (GenreTree.ParsedGenre g : GenreTree.parseList(readDefaults())) {
            Genre top = getOrCreate(user.getId(), null, g.name(), created, 0);
            g.subgenres().forEach(s -> getOrCreate(user.getId(), top, s.name(), created, 1));
        }
        user.setGenresInitialized(true);
    }

    private Genre getOrCreate(UUID userId, Genre parent, String name, int[] created, int level) {
        String path = (parent == null ? "" : parent.getPath() + Genre.PATH_SEPARATOR)
                + name.strip().toLowerCase(Locale.ROOT);
        return genreRepository.findByUserIdAndPath(userId, path).orElseGet(() -> {
            created[level]++;
            return genreRepository.save(new Genre(userId, parent, name.length() > 100 ? name.substring(0, 100) : name));
        });
    }

    private void assertUnique(UUID userId, String path, UUID self) {
        genreRepository.findByUserIdAndPath(userId, path)
                .filter(other -> !other.getId().equals(self))
                .ifPresent(other -> {
                    throw new InvalidRequestException("Dit genre bestaat al.");
                });
    }

    private static String readDefaults() {
        try (InputStream in = GenreService.class.getResourceAsStream(DEFAULT_GENRES)) {
            if (in == null) {
                throw new IllegalStateException("Standaardgenres niet gevonden: " + DEFAULT_GENRES);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
