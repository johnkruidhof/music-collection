package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import nl.johnkruidhof.musiccollection.dto.admin.AdminReleaseDto;
import nl.johnkruidhof.musiccollection.dto.admin.AdminReleasePageDto;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.wishlist.AddToWishlistRequest;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/** Catalogusbeheer (F-27) tegen Postgres. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AdminCatalogServiceIntegrationTest {

    private static final Jwt ALICE = jwt("alice-sub", "alice");
    private static final Jwt BOB = jwt("bob-sub", "bob");

    @Autowired
    private AdminCatalogService adminCatalogService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ExternalLookupService externalLookupService;

    @BeforeEach
    void setUp() {
        TestDatabase.clean(jdbc);
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "1")).willReturn(release("1", "OK Computer", "724385522925"));
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "2")).willReturn(release("2", "OK Computer (dubbel)", null));
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "3")).willReturn(release("3", "Kid A", null));
    }

    @Test
    void searchShowsUsageAndFindsByTextOrExactId() {
        collectionService.add(ALICE, fromDiscogs("1"));
        collectionService.add(BOB, fromDiscogs("1"));
        wishlistService.add(BOB, new AddToWishlistRequest(MetadataSource.DISCOGS, "3", null));

        AdminReleasePageDto all = adminCatalogService.search(null, null, 0, 25);
        assertThat(all.content()).extracting(AdminReleaseDto::title, AdminReleaseDto::collectionCount,
                AdminReleaseDto::wishlistCount).containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Kid A", 0L, 1L),
                        org.assertj.core.groups.Tuple.tuple("OK Computer", 2L, 0L));
        assertThat(all.unusedCount()).isZero();

        assertThat(adminCatalogService.search("COMPUTER", null, 0, 25).content()).hasSize(1);
        assertThat(adminCatalogService.search("724385522925", null, 0, 25).content()).hasSize(1);
        assertThat(adminCatalogService.search(null, MetadataSource.MANUAL, 0, 25).content()).isEmpty();
    }

    @Test
    void mergeMovesItemsAndWishesAndDeletesSource() {
        collectionService.add(ALICE, fromDiscogs("2"));
        wishlistService.add(ALICE, new AddToWishlistRequest(MetadataSource.DISCOGS, "2", null));
        wishlistService.add(BOB, new AddToWishlistRequest(MetadataSource.DISCOGS, "2", null));
        wishlistService.add(BOB, new AddToWishlistRequest(MetadataSource.DISCOGS, "1", null));
        UUID source = releaseRepository.findByDiscogsId("2").orElseThrow().getId();
        UUID target = releaseRepository.findByDiscogsId("1").orElseThrow().getId();

        AdminReleaseDto merged = adminCatalogService.merge(source, target);

        assertThat(merged.id()).isEqualTo(target);
        assertThat(merged.collectionCount()).isEqualTo(1);
        // Alice' wens verhuist; Bob had het doel al op zijn lijst, dus zijn tweede wens vervalt.
        assertThat(merged.wishlistCount()).isEqualTo(2);
        assertThat(releaseRepository.findById(source)).isEmpty();
        assertThat(collectionService.list(ALICE, null, null, null, 0, 24).content())
                .extracting("title").containsExactly("OK Computer");
    }

    @Test
    void onlyUnusedReleasesCanBeDeleted() {
        collectionService.add(ALICE, fromDiscogs("1"));
        UUID used = releaseRepository.findByDiscogsId("1").orElseThrow().getId();
        wishlistService.add(ALICE, new AddToWishlistRequest(MetadataSource.DISCOGS, "3", null));
        wishlistService.delete(ALICE, wishlistService.list(ALICE).getFirst().id());
        UUID unused = releaseRepository.findByDiscogsId("3").orElseThrow().getId();

        assertThatThrownBy(() -> adminCatalogService.delete(used)).isInstanceOf(InvalidRequestException.class);
        assertThat(adminCatalogService.search(null, null, 0, 25).unusedCount()).isEqualTo(1);

        assertThat(adminCatalogService.deleteUnused()).isEqualTo(1);
        assertThat(releaseRepository.findById(unused)).isEmpty();
        assertThat(releaseRepository.findById(used)).isPresent();
    }

    @Test
    void mergeWithItselfIsRejected() {
        collectionService.add(ALICE, fromDiscogs("1"));
        UUID id = releaseRepository.findByDiscogsId("1").orElseThrow().getId();

        assertThatThrownBy(() -> adminCatalogService.merge(id, id)).isInstanceOf(InvalidRequestException.class);
    }

    private static AddToCollectionRequest fromDiscogs(String id) {
        return new AddToCollectionRequest(MetadataSource.DISCOGS, id, null, null);
    }

    private static Optional<ReleaseDto> release(String discogsId, String title, String barcode) {
        return Optional.of(new ReleaseDto(null, barcode, "Radiohead", title, 1997, MediaType.CD, null, null, null,
                null, null, null, MetadataSource.DISCOGS, discogsId, null, List.of()));
    }

    private static Jwt jwt(String subject, String username) {
        return Jwt.withTokenValue("token-" + UUID.randomUUID()).header("alg", "none")
                .subject(subject).claim("preferred_username", username).build();
    }
}
