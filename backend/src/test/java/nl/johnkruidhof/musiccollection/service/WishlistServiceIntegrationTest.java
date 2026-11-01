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
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.wishlist.AddToWishlistRequest;
import nl.johnkruidhof.musiccollection.dto.wishlist.WishlistItemDto;
import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.repository.AppUserRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;
import nl.johnkruidhof.musiccollection.repository.WishlistItemRepository;

/** Verlanglijst (F-24) tegen Postgres. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class WishlistServiceIntegrationTest {

    private static final Jwt ALICE = jwt("alice-sub", "alice");
    private static final Jwt BOB = jwt("bob-sub", "bob");

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private OwnershipService ownershipService;

    @Autowired
    private UserService userService;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

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
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "1")).willReturn(Optional.of(new ReleaseDto(
                null, null, "Radiohead", "Kid A", 2000, MediaType.VINYL, "2×Vinyl", "Parlophone", null, "UK", null,
                null, MetadataSource.DISCOGS, "1", null, List.of())));
    }

    @Test
    void addIsIdempotentAndPersonal() {
        WishlistItemDto first = wishlistService.add(ALICE, new AddToWishlistRequest(MetadataSource.DISCOGS, "1", " cadeau ", null));
        WishlistItemDto again = wishlistService.add(ALICE, new AddToWishlistRequest(MetadataSource.DISCOGS, "1", null, null));

        assertThat(again.id()).isEqualTo(first.id());
        assertThat(first.title()).isEqualTo("Kid A");
        assertThat(first.notes()).isEqualTo("cadeau");
        assertThat(wishlistService.list(ALICE)).hasSize(1);
        assertThat(wishlistService.list(BOB)).isEmpty();
        assertThatThrownBy(() -> wishlistService.delete(BOB, first.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void candidatesShowWhetherTheyAreOnTheWishlist() {
        wishlistService.add(ALICE, new AddToWishlistRequest(MetadataSource.DISCOGS, "1", null, null));
        UUID alice = userService.getOrCreate(ALICE).getId();
        UUID bob = userService.getOrCreate(BOB).getId();
        List<ReleaseCandidateDto> candidates = List.of(new ReleaseCandidateDto(MetadataSource.DISCOGS, "1", "Radiohead",
                "Kid A", 2000, MediaType.VINYL, null, null, null, null, null, null, null, null));

        assertThat(ownershipService.withOwnedCounts(alice, candidates).getFirst().onWishlist()).isTrue();
        assertThat(ownershipService.withOwnedCounts(bob, candidates).getFirst().onWishlist()).isFalse();
    }

    @Test
    void acquireMovesTheWishToTheCollection() {
        WishlistItemDto wish = wishlistService.add(ALICE, new AddToWishlistRequest(MetadataSource.DISCOGS, "1", null, null));

        CollectionItemDto item = wishlistService.acquire(ALICE, wish.id(),
                new CollectionItemFields(MediaCondition.MINT, null, null, null, null, "Kast C", null, null));

        assertThat(item.release().title()).isEqualTo("Kid A");
        assertThat(item.conditionMedia()).isEqualTo(MediaCondition.MINT);
        assertThat(wishlistService.list(ALICE)).isEmpty();
        assertThat(collectionItemRepository.count()).isEqualTo(1);
    }

    @Test
    void deleteRemovesOnlyTheWish() {
        WishlistItemDto wish = wishlistService.add(ALICE, new AddToWishlistRequest(MetadataSource.DISCOGS, "1", null, null));

        wishlistService.delete(ALICE, wish.id());

        assertThat(wishlistService.list(ALICE)).isEmpty();
        assertThat(releaseRepository.count()).isEqualTo(1);
    }

    private static Jwt jwt(String subject, String username) {
        return Jwt.withTokenValue("token-" + UUID.randomUUID()).header("alg", "none")
                .subject(subject).claim("preferred_username", username).build();
    }
}
