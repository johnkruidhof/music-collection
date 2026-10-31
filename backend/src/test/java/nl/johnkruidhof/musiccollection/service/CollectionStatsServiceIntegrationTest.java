package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.util.List;
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
import nl.johnkruidhof.musiccollection.TestcontainersConfiguration;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
import nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto;
import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.repository.AppUserRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CollectionStatsServiceIntegrationTest {

    private static final Jwt ALICE = jwt("alice-sub", "alice");
    private static final Jwt BOB = jwt("bob-sub", "bob");

    @Autowired
    private CollectionStatsService statsService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CollectionItemRepository collectionItemRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ExternalLookupService externalLookupService;

    @BeforeEach
    void cleanDatabase() {
        TestDatabase.clean(jdbc);
    }

    @Test
    void statsAreAggregatedPerUser() {
        add(ALICE, "Radiohead", 1997, MediaType.VINYL, "29.99", MediaCondition.NEAR_MINT);
        add(ALICE, "Radiohead", 2000, MediaType.CD, "12.50", MediaCondition.NEAR_MINT);
        add(ALICE, "Portishead", 1994, MediaType.VINYL, null, MediaCondition.GOOD);
        add(ALICE, "De Dijk", null, MediaType.CASSETTE, null, null);
        add(BOB, "Bob Dylan", 1966, MediaType.VINYL, "100.00", MediaCondition.POOR);

        CollectionStatsDto stats = statsService.getStats(ALICE);

        assertThat(stats.totalItems()).isEqualTo(4);
        assertThat(stats.totalValue()).isEqualByComparingTo("42.49");
        assertThat(stats.itemsWithPrice()).isEqualTo(2);
        assertThat(stats.byMediaType()).extracting("mediaType", "count").containsExactly(
                tuple(MediaType.VINYL, 2L), tuple(MediaType.CASSETTE, 1L), tuple(MediaType.CD, 1L));
        assertThat(stats.byDecade()).extracting("decade", "count").containsExactly(
                tuple(1990, 2L), tuple(2000, 1L), tuple(null, 1L));
        assertThat(stats.topArtists()).extracting("artist", "count").containsExactly(
                tuple("Radiohead", 2L), tuple("De Dijk", 1L), tuple("Portishead", 1L));
        assertThat(stats.byCondition()).extracting("condition", "count").containsExactly(
                tuple(MediaCondition.NEAR_MINT, 2L), tuple(MediaCondition.GOOD, 1L), tuple(null, 1L));
    }

    @Test
    void emptyCollectionHasZeroValue() {
        CollectionStatsDto stats = statsService.getStats(BOB);

        assertThat(stats.totalItems()).isZero();
        assertThat(stats.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.byMediaType()).isEmpty();
    }

    private void add(Jwt jwt, String artist, Integer year, MediaType type, String price, MediaCondition condition) {
        collectionService.add(jwt, new AddToCollectionRequest(null, null,
                new ManualReleaseRequest(null, artist, "Titel", year, type, null, null, null, null, List.of()),
                new CollectionItemFields(condition, null, null, price == null ? null : new BigDecimal(price),
                        null, null, null)));
    }

    private static Jwt jwt(String subject, String username) {
        return Jwt.withTokenValue("token-" + UUID.randomUUID()).header("alg", "none")
                .subject(subject).claim("preferred_username", username).build();
    }
}
