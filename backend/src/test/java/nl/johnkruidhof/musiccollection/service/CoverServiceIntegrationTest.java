package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

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
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.exception.UnsupportedImageException;
import nl.johnkruidhof.musiccollection.repository.AppUserRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.CoverImageRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/** Cover-upload en het bewerken van handmatige releases tegen een echte Postgres. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CoverServiceIntegrationTest {

    private static final Jwt ALICE = jwt("alice-sub", "alice");
    private static final Jwt BOB = jwt("bob-sub", "bob");

    @Autowired
    private CoverService coverService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CollectionItemRepository collectionItemRepository;

    @Autowired
    private CoverImageRepository coverImageRepository;

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
    void uploadStoresScaledImageAndThumbnailAndSetsUrls() throws IOException {
        CollectionItemDto item = addManual(ALICE);

        CollectionItemDto withCover = coverService.upload(ALICE, item.id(), png(2400, 1600));

        String url = withCover.release().coverImageUrl();
        assertThat(url).startsWith("/api/covers/");
        assertThat(withCover.release().coverThumbnailUrl()).isEqualTo(url + "/thumbnail");
        UUID coverId = UUID.fromString(url.substring("/api/covers/".length()));

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(coverService.get(coverId, false).data()));
        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(coverService.get(coverId, true).data()));
        assertThat(image.getWidth()).isEqualTo(1200);
        assertThat(image.getHeight()).isEqualTo(800);
        assertThat(thumbnail.getWidth()).isEqualTo(300);
        assertThat(coverService.get(coverId, true).contentType()).isEqualTo("image/jpeg");
        // Ook na opnieuw ophalen staan de URL's er (opgeslagen, niet alleen in het antwoord).
        assertThat(collectionService.get(ALICE, item.id()).release().coverImageUrl()).isEqualTo(url);
    }

    @Test
    void newUploadReplacesOldCoverWithNewUrl() throws IOException {
        CollectionItemDto item = addManual(ALICE);
        String first = coverService.upload(ALICE, item.id(), png(400, 400)).release().coverImageUrl();

        String second = coverService.upload(ALICE, item.id(), png(500, 500)).release().coverImageUrl();

        assertThat(second).isNotEqualTo(first);
        assertThat(coverImageRepository.count()).isEqualTo(1);
    }

    @Test
    void deleteRemovesCover() throws IOException {
        CollectionItemDto item = addManual(ALICE);
        coverService.upload(ALICE, item.id(), png(400, 400));

        CollectionItemDto withoutCover = coverService.delete(ALICE, item.id());

        assertThat(withoutCover.release().coverImageUrl()).isNull();
        assertThat(coverImageRepository.count()).isZero();
    }

    @Test
    void deletingTheItemAlsoDeletesTheCover() throws IOException {
        CollectionItemDto item = addManual(ALICE);
        coverService.upload(ALICE, item.id(), png(400, 400));

        collectionService.delete(ALICE, item.id());

        assertThat(coverImageRepository.count()).isZero();
    }

    @Test
    void onlyManualReleasesOfTheOwnerAcceptACover() throws IOException {
        CollectionItemDto manual = addManual(ALICE);
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "1")).willReturn(Optional.of(new ReleaseDto(
                null, null, "Radiohead", "Kid A", 2000, MediaType.CD, null, null, null, null, null, null,
                MetadataSource.DISCOGS, "1", null, List.of())));
        CollectionItemDto imported = collectionService.add(ALICE,
                new AddToCollectionRequest(MetadataSource.DISCOGS, "1", null, null));
        byte[] png = png(100, 100);

        assertThatThrownBy(() -> coverService.upload(ALICE, imported.id(), png)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> coverService.upload(BOB, manual.id(), png)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> coverService.upload(ALICE, manual.id(), "geen plaatje".getBytes()))
                .isInstanceOf(UnsupportedImageException.class);
    }

    @Test
    void manualReleaseCanBeEditedIncludingTracks() {
        CollectionItemDto item = addManual(ALICE);

        CollectionItemDto updated = collectionService.updateRelease(ALICE, item.id(), new ManualReleaseRequest(
                "0 77774-6439 2", "De Dijk", "Wakker in een vreemde wereld", 1994, MediaType.CD, "CD, Album",
                "Polydor", "523 456-2", "NL", List.of(new TrackDto(1, "1", 1, "Ik kan het niet alleen", 230, null))));

        assertThat(updated.release().id()).isEqualTo(item.release().id());
        assertThat(updated.release().title()).isEqualTo("Wakker in een vreemde wereld");
        assertThat(updated.release().barcode()).isEqualTo("07777464392");
        assertThat(updated.release().source()).isEqualTo(MetadataSource.MANUAL);
        assertThat(updated.release().tracks()).extracting(TrackDto::title).containsExactly("Ik kan het niet alleen");
        assertThat(collectionService.get(ALICE, item.id()).release().tracks()).hasSize(1);
    }

    @Test
    void importedReleaseCannotBeEdited() {
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "1")).willReturn(Optional.of(new ReleaseDto(
                null, null, "Radiohead", "Kid A", 2000, MediaType.CD, null, null, null, null, null, null,
                MetadataSource.DISCOGS, "1", null, List.of())));
        CollectionItemDto imported = collectionService.add(ALICE,
                new AddToCollectionRequest(MetadataSource.DISCOGS, "1", null, null));

        assertThatThrownBy(() -> collectionService.updateRelease(ALICE, imported.id(), manualRequest()))
                .isInstanceOf(InvalidRequestException.class);
    }

    private CollectionItemDto addManual(Jwt jwt) {
        return collectionService.add(jwt, new AddToCollectionRequest(null, null, manualRequest(), null));
    }

    private static ManualReleaseRequest manualRequest() {
        return new ManualReleaseRequest(null, "De Dijk", "Nooit meer slapen", 1985, MediaType.VINYL, null, null,
                null, null, List.of(new TrackDto(1, "A1", 1, "Mag het licht uit", null, null),
                        new TrackDto(1, "A2", 2, "Zoals je bent", null, null)));
    }

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static Jwt jwt(String subject, String username) {
        return Jwt.withTokenValue("token-" + UUID.randomUUID()).header("alg", "none")
                .subject(subject).claim("preferred_username", username).build();
    }
}
