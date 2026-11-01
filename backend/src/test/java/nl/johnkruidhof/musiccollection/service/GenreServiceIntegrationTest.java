package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import nl.johnkruidhof.musiccollection.TestDatabase;
import nl.johnkruidhof.musiccollection.TestFixtures;
import nl.johnkruidhof.musiccollection.TestcontainersConfiguration;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemSummaryDto;
import nl.johnkruidhof.musiccollection.dto.genre.GenreDto;
import nl.johnkruidhof.musiccollection.dto.genre.GenreImportResultDto;
import nl.johnkruidhof.musiccollection.dto.genre.GenreRequest;
import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto;
import nl.johnkruidhof.musiccollection.dto.wishlist.AddToWishlistRequest;
import nl.johnkruidhof.musiccollection.dto.wishlist.WishlistItemDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;

/** Genres (F-28) tegen Postgres: standaardlijst, beheer, boomstructuur, voorstel, filter, sortering, stats. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class GenreServiceIntegrationTest {

    private static final Jwt ALICE = jwt("alice-sub", "alice");
    private static final Jwt BOB = jwt("bob-sub", "bob");

    @Autowired
    private GenreService genreService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private CollectionStatsService statsService;

    @Autowired
    private CollectionExportService exportService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ExternalLookupService externalLookupService;

    @BeforeEach
    void setUp() {
        TestDatabase.clean(jdbc);
    }

    @Test
    void newUserGetsDefaultGenresOnceAndOwnListIsPersonal() {
        List<GenreDto> genres = genreService.list(ALICE);

        assertThat(genres).hasSize(14);
        assertThat(genres).extracting(GenreDto::name).startsWith("Alternatieve rock", "Blues-Rock");
        GenreDto metal = find(genres, "Hardrock - Metal");
        assertThat(metal.subgenres()).hasSize(10).extracting(GenreDto::name).contains("Doom Metal", "Thrash Metal");

        genreService.delete(ALICE, find(genres, "Jazz").id());
        assertThat(genreService.list(ALICE)).hasSize(13); // niet opnieuw aangevuld
        assertThat(genreService.list(BOB)).hasSize(14);    // eigen lijst
    }

    @Test
    void genresCanBeCreatedRenamedAndAreUniquePerLevel() {
        GenreDto rock = find(genreService.list(ALICE), "Rock");

        GenreDto sub = genreService.create(ALICE, new GenreRequest("Psychedelica", rock.id()));
        assertThatThrownBy(() -> genreService.create(ALICE, new GenreRequest("psychedelica", rock.id())))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> genreService.create(ALICE, new GenreRequest("Dieper", sub.id())))
                .isInstanceOf(InvalidRequestException.class);
        // Andermans genre bestaat voor jou niet.
        assertThatThrownBy(() -> genreService.create(BOB, new GenreRequest("X", rock.id())))
                .isInstanceOf(InvalidRequestException.class);

        genreService.rename(ALICE, rock.id(), new GenreRequest("Rock & Pop", null));
        GenreDto renamed = find(genreService.list(ALICE), "Rock & Pop");
        assertThat(renamed.subgenres()).extracting(GenreDto::name).containsExactly("Psychedelica");
    }

    @Test
    void importedTreeCreatesGenresLinksArtistsAndAssignsExistingItems() {
        add(ALICE, "Tool", "Lateralus", null);
        add(ALICE, "The Book of Knots", "Traineater", null);
        add(ALICE, "Onbekend", "Iets", null);
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "7")).willReturn(Optional.of(new ReleaseDto(
                null, null, "Candlemass", "Nightfall", 1987, MediaType.VINYL, null, null, null, null, null, null,
                MetadataSource.DISCOGS, "7", null, List.of())));
        WishlistItemDto wish = wishlistService.add(ALICE, new AddToWishlistRequest(MetadataSource.DISCOGS, "7", null, null));
        assertThat(wish.genreName()).isNull();

        GenreImportResultDto result = importTree(ALICE);

        // Alle genres van de boom bestaan al als standaardgenre, behalve niets: 0 nieuw.
        assertThat(result.genresCreated()).isZero();
        assertThat(result.artistsLinked()).isEqualTo(7);
        assertThat(result.itemsAssigned()).isEqualTo(2);
        assertThat(result.wishesAssigned()).isEqualTo(1);
        assertThat(items(ALICE, null, null)).extracting(CollectionItemSummaryDto::artist,
                CollectionItemSummaryDto::genreName, CollectionItemSummaryDto::subgenreName).containsExactlyInAnyOrder(
                        tuple("Tool", "Hardrock - Metal", "Progressive Metal"),
                        tuple("The Book of Knots", "Alternatieve rock", null),
                        tuple("Onbekend", null, null));
        assertThat(wishlistService.list(ALICE).getFirst().subgenreName()).isEqualTo("Doom Metal");

        // Nog een keer importeren verandert niets.
        assertThat(importTree(ALICE)).isEqualTo(new GenreImportResultDto(0, 0, 0, 0, 0));
    }

    @Test
    void genreIsSuggestedByArtistAndLearnedFromOwnChoice() {
        importTree(ALICE);
        CollectionItemDto tool = add(ALICE, "Tool", "Fear Inoculum", null);
        assertThat(tool.subgenreName()).isEqualTo("Progressive Metal");

        CollectionItemDto first = add(ALICE, "Opeth", "Damnation", null);
        assertThat(first.genreId()).isNull();
        UUID doom = find(find(genreService.list(ALICE), "Hardrock - Metal").subgenres(), "Doom Metal").id();
        collectionService.update(ALICE, first.id(), fields(doom));

        assertThat(add(ALICE, "Opeth", "Blackwater Park", null).subgenreName()).isEqualTo("Doom Metal");
    }

    @Test
    void collectionCanBeFilteredAndSortedByGenre() {
        importTree(ALICE);
        add(ALICE, "Tool", "Lateralus", null);
        add(ALICE, "Candlemass", "Nightfall", null);
        add(ALICE, "A Perfect Circle", "Thirteenth Step", null);
        add(ALICE, "Onbekend", "Iets", null);
        GenreDto metal = find(genreService.list(ALICE), "Hardrock - Metal");

        // Filter op hoofdgenre neemt de subgenres mee.
        assertThat(items(ALICE, metal.id().toString(), "artist")).extracting(CollectionItemSummaryDto::artist)
                .containsExactly("Candlemass", "Tool");
        assertThat(items(ALICE, CollectionService.NO_GENRE, null)).extracting(CollectionItemSummaryDto::artist)
                .containsExactly("Onbekend");
        // Sorteren op genre: per genre/subgenre gegroepeerd, zonder genre achteraan.
        assertThat(items(ALICE, null, "genre")).extracting(CollectionItemSummaryDto::artist)
                .containsExactly("A Perfect Circle", "Candlemass", "Tool", "Onbekend");
        assertThatThrownBy(() -> items(ALICE, "rock", null)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void statsAndExportIncludeGenre() {
        importTree(ALICE);
        add(ALICE, "Tool", "Lateralus", null);
        add(ALICE, "Candlemass", "Nightfall", null);
        add(ALICE, "Onbekend", "Iets", null);

        CollectionStatsDto stats = statsService.getStats(ALICE);
        assertThat(stats.byGenre()).extracting("genre", "count")
                .containsExactly(tuple("Hardrock - Metal", 2L), tuple(null, 1L));

        List<List<String>> rows = Csv.parse(exportService.exportCsv(ALICE), ';');
        assertThat(rows.getFirst()).endsWith("Genre", "Subgenre");
        assertThat(rows).anySatisfy(row -> assertThat(row).startsWith("Tool").endsWith("Hardrock - Metal",
                "Progressive Metal"));
    }

    @Test
    void deletingAGenreKeepsItemsWithoutGenre() {
        importTree(ALICE);
        CollectionItemDto tool = add(ALICE, "Tool", "Lateralus", null);

        genreService.delete(ALICE, find(genreService.list(ALICE), "Hardrock - Metal").id());

        assertThat(collectionService.get(ALICE, tool.id()).genreId()).isNull();
    }

    private GenreImportResultDto importTree(Jwt jwt) {
        return genreService.importTree(jwt,
                TestFixtures.read("genres/boomstructuur-voorbeeld.txt").getBytes(StandardCharsets.UTF_8));
    }

    private CollectionItemDto add(Jwt jwt, String artist, String title, UUID genreId) {
        return collectionService.add(jwt, new AddToCollectionRequest(null, null,
                new ManualReleaseRequest(null, artist, title, 2000, MediaType.CD, null, null, null, null, List.of()),
                genreId == null ? null : fields(genreId)));
    }

    private List<CollectionItemSummaryDto> items(Jwt jwt, String genre, String sort) {
        return collectionService.list(jwt, null, null, genre, sort, 0, 50).content();
    }

    private static CollectionItemFields fields(UUID genreId) {
        return new CollectionItemFields(null, null, null, null, null, null, null, genreId);
    }

    private static GenreDto find(List<GenreDto> genres, String name) {
        return genres.stream().filter(g -> g.name().equals(name)).findFirst().orElseThrow();
    }

    private static Jwt jwt(String subject, String username) {
        return Jwt.withTokenValue("token-" + UUID.randomUUID()).header("alg", "none")
                .subject(subject).claim("preferred_username", username).build();
    }
}
