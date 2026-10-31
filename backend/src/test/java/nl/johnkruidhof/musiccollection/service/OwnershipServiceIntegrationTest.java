package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
import nl.johnkruidhof.musiccollection.TestcontainersConfiguration;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.repository.AppUserRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/** Dubbelen-detectie (F-23) tegen Postgres. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OwnershipServiceIntegrationTest {

    private static final Jwt ALICE = jwt("alice-sub", "alice");
    private static final Jwt BOB = jwt("bob-sub", "bob");
    private static final String MBID = "6a09041b-0f79-3278-88d9-0552dd28d37f";

    @Autowired
    private OwnershipService ownershipService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private UserService userService;

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
    void setUp() {
        TestDatabase.clean(jdbc);
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "1")).willReturn(Optional.of(release(
                MetadataSource.DISCOGS, "1", null)));
        given(externalLookupService.getDetails(MetadataSource.MUSICBRAINZ, MBID)).willReturn(Optional.of(release(
                MetadataSource.MUSICBRAINZ, null, MBID)));
    }

    @Test
    void candidatesGetTheNumberOfOwnCopies() {
        add(ALICE, MetadataSource.DISCOGS, "1");
        add(ALICE, MetadataSource.DISCOGS, "1");
        add(ALICE, MetadataSource.MUSICBRAINZ, MBID);
        add(BOB, MetadataSource.DISCOGS, "1");

        List<ReleaseCandidateDto> result = ownershipService.withOwnedCounts(userService.getOrCreate(ALICE).getId(),
                List.of(candidate(MetadataSource.DISCOGS, "1"), candidate(MetadataSource.DISCOGS, "2"),
                        candidate(MetadataSource.MUSICBRAINZ, MBID)));

        assertThat(result).extracting(ReleaseCandidateDto::ownedCount).containsExactly(2, 0, 1);
    }

    @Test
    void worksWithOnlyOneSourceAndEmptyList() {
        UUID bob = userService.getOrCreate(BOB).getId();

        assertThat(ownershipService.withOwnedCounts(bob, List.of(candidate(MetadataSource.MUSICBRAINZ, MBID))))
                .extracting(ReleaseCandidateDto::ownedCount).containsExactly(0);
        assertThat(ownershipService.withOwnedCounts(bob, List.of())).isEmpty();
    }

    private void add(Jwt jwt, MetadataSource source, String externalId) {
        collectionService.add(jwt, new AddToCollectionRequest(source, externalId, null, null));
    }

    private static ReleaseCandidateDto candidate(MetadataSource source, String externalId) {
        return new ReleaseCandidateDto(source, externalId, "Radiohead", "OK Computer", 1997, MediaType.CD, null, null,
                null, null, null, null, null, null);
    }

    private static ReleaseDto release(MetadataSource source, String discogsId, String musicbrainzId) {
        return new ReleaseDto(null, null, "Radiohead", "OK Computer", 1997, MediaType.CD, null, null, null, null, null,
                null, source, discogsId, musicbrainzId, List.of());
    }

    private static Jwt jwt(String subject, String username) {
        return Jwt.withTokenValue("token-" + UUID.randomUUID()).header("alg", "none")
                .subject(subject).claim("preferred_username", username).build();
    }
}
