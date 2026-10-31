package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import nl.johnkruidhof.musiccollection.TestcontainersConfiguration;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemSummaryDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionPageDto;
import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.repository.AppUserRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/** Collectie-API tegen een echte Postgres (Testcontainers); externe bronnen zijn gemockt. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CollectionServiceIntegrationTest {

    private static final Jwt ALICE = jwt("alice-sub", "alice");
    private static final Jwt BOB = jwt("bob-sub", "bob");

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CollectionItemRepository collectionItemRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private AppUserRepository userRepository;

    @MockitoBean
    private ExternalLookupService externalLookupService;

    @BeforeEach
    void cleanDatabase() {
        collectionItemRepository.deleteAll();
        releaseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void addFromSourceImportsReleaseOnceAndSharesItInTheCatalog() {
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "2149389")).willReturn(Optional.of(okComputer()));

        CollectionItemDto first = collectionService.add(ALICE, new AddToCollectionRequest(MetadataSource.DISCOGS,
                "2149389", null, new CollectionItemFields(MediaCondition.NEAR_MINT, null, LocalDate.of(2024, 5, 1),
                        new BigDecimal("29.99"), "Concerto", "Kast A", "Eerste persing?")));
        CollectionItemDto second = collectionService.add(BOB,
                new AddToCollectionRequest(MetadataSource.DISCOGS, "2149389", null, null));

        assertThat(first.release().id()).isNotNull().isEqualTo(second.release().id());
        assertThat(first.release().tracks()).extracting(TrackDto::title).containsExactly("Airbag", "Paranoid Android");
        assertThat(first.conditionMedia()).isEqualTo(MediaCondition.NEAR_MINT);
        assertThat(first.purchasePrice()).isEqualByComparingTo("29.99");
        assertThat(releaseRepository.count()).isEqualTo(1);
        verify(externalLookupService, times(1)).getDetails(MetadataSource.DISCOGS, "2149389");
    }

    @Test
    void manualReleaseIsCreatedWithTracksInGivenOrder() {
        CollectionItemDto item = collectionService.add(ALICE, new AddToCollectionRequest(null, null,
                new ManualReleaseRequest("8 712345 678906", "De Dijk", "Live in Paradiso", 1990, MediaType.CASSETTE,
                        null, null, null, "NL", List.of(
                                new TrackDto(1, "A1", 9, "Dansen op de vulkaan", 250, null),
                                new TrackDto(1, "A2", 3, "Mag het licht uit", null, null))),
                null));

        assertThat(item.release().source()).isEqualTo(MetadataSource.MANUAL);
        assertThat(item.release().barcode()).isEqualTo("8712345678906");
        assertThat(item.release().tracks()).extracting(TrackDto::sortOrder, TrackDto::title)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1, "Dansen op de vulkaan"),
                        org.assertj.core.groups.Tuple.tuple(2, "Mag het licht uit"));
    }

    @Test
    void requestMustBeEitherSourceOrManual() {
        assertThatThrownBy(() -> collectionService.add(ALICE, new AddToCollectionRequest(null, null, null, null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> collectionService.add(ALICE,
                new AddToCollectionRequest(MetadataSource.DISCOGS, "1", manual("X", "Y", MediaType.CD), null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> collectionService.add(ALICE,
                new AddToCollectionRequest(MetadataSource.DISCOGS, null, null, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void usersOnlySeeTheirOwnItems() {
        CollectionItemDto alicesItem = addManual(ALICE, "Radiohead", "Kid A", MediaType.CD);
        addManual(BOB, "Bob Dylan", "Blonde on Blonde", MediaType.VINYL);

        assertThat(collectionService.list(ALICE, null, null, null, 0, 24).content())
                .extracting(CollectionItemSummaryDto::title).containsExactly("Kid A");
        // Andermans item bestaat voor jou niet: 404, geen 403.
        assertThatThrownBy(() -> collectionService.get(BOB, alicesItem.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> collectionService.update(BOB, alicesItem.id(), CollectionItemFields.EMPTY))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> collectionService.delete(BOB, alicesItem.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listFiltersSearchesSortsAndPaginates() {
        addManual(ALICE, "Radiohead", "OK Computer", MediaType.VINYL);
        addManual(ALICE, "Radiohead", "Kid A", MediaType.CD);
        addManual(ALICE, "Portishead", "Dummy", MediaType.VINYL);
        addManual(ALICE, "Björk", "100%_Pure", MediaType.CD);

        assertThat(titles(collectionService.list(ALICE, "head", null, "title", 0, 24)))
                .containsExactly("Dummy", "Kid A", "OK Computer");
        assertThat(titles(collectionService.list(ALICE, "RADIO", MediaType.VINYL, null, 0, 24)))
                .containsExactly("OK Computer");
        // % en _ zijn letterlijke tekens, geen wildcards.
        assertThat(titles(collectionService.list(ALICE, "0%_", null, null, 0, 24))).containsExactly("100%_Pure");
        assertThat(titles(collectionService.list(ALICE, null, null, "artist,desc", 0, 24)))
                .containsExactly("Kid A", "OK Computer", "Dummy", "100%_Pure");

        CollectionPageDto page = collectionService.list(ALICE, null, null, "artist", 1, 3);
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(titles(page)).containsExactly("OK Computer");
    }

    @Test
    void invalidSortOrPageIsRejected() {
        assertThatThrownBy(() -> collectionService.list(ALICE, null, null, "user.id", 0, 24))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> collectionService.list(ALICE, null, null, "artist,sideways", 0, 24))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> collectionService.list(ALICE, null, null, null, 0, 500))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void updateOverwritesPersonalFields() {
        CollectionItemDto item = collectionService.add(ALICE, new AddToCollectionRequest(null, null,
                manual("Radiohead", "Kid A", MediaType.CD),
                new CollectionItemFields(MediaCondition.GOOD, null, null, null, null, "Zolder", "oud")));

        CollectionItemDto updated = collectionService.update(ALICE, item.id(), new CollectionItemFields(
                MediaCondition.VERY_GOOD_PLUS, MediaCondition.GOOD, null, null, null, "Kast B", null));

        assertThat(updated.conditionMedia()).isEqualTo(MediaCondition.VERY_GOOD_PLUS);
        assertThat(updated.storageLocation()).isEqualTo("Kast B");
        assertThat(updated.notes()).isNull();
    }

    @Test
    void deletingLastItemOfManualReleaseRemovesReleaseButKeepsImportedOnes() {
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "2149389")).willReturn(Optional.of(okComputer()));
        CollectionItemDto imported = collectionService.add(ALICE,
                new AddToCollectionRequest(MetadataSource.DISCOGS, "2149389", null, null));
        CollectionItemDto manual = addManual(ALICE, "De Dijk", "Nooit meer slapen", MediaType.CD);

        collectionService.delete(ALICE, manual.id());
        collectionService.delete(ALICE, imported.id());

        assertThat(collectionItemRepository.count()).isZero();
        assertThat(releaseRepository.findById(manual.release().id())).isEmpty();
        assertThat(releaseRepository.findById(imported.release().id())).isPresent();
    }

    private CollectionItemDto addManual(Jwt jwt, String artist, String title, MediaType mediaType) {
        return collectionService.add(jwt, new AddToCollectionRequest(null, null, manual(artist, title, mediaType), null));
    }

    private static ManualReleaseRequest manual(String artist, String title, MediaType mediaType) {
        return new ManualReleaseRequest(null, artist, title, null, mediaType, null, null, null, null, List.of());
    }

    private static List<String> titles(CollectionPageDto page) {
        return page.content().stream().map(CollectionItemSummaryDto::title).toList();
    }

    private static ReleaseDto okComputer() {
        return new ReleaseDto(null, "724385522925", "Radiohead", "OK Computer", 1997, MediaType.VINYL,
                "2×Vinyl, LP, Album", "Parlophone", "NODATA 02", "UK", "https://img/cover.jpg", "https://img/thumb.jpg",
                MetadataSource.DISCOGS, "2149389", null, List.of(
                        new TrackDto(1, "A1", 1, "Airbag", 284, null),
                        new TrackDto(1, "A2", 2, "Paranoid Android", 383, null)));
    }

    private static Jwt jwt(String subject, String username) {
        return Jwt.withTokenValue("token-" + UUID.randomUUID()).header("alg", "none")
                .subject(subject).claim("preferred_username", username).build();
    }
}
