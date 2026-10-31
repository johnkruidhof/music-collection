package nl.johnkruidhof.musiccollection.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import nl.johnkruidhof.musiccollection.TestcontainersConfiguration;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.CoverImageRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/** Covers cachen tegen Postgres, met WireMock als image-CDN. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CoverCacheServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension cdn = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @Autowired
    private CoverCacheService coverCacheService;

    @Autowired
    private CoverService coverService;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private CoverImageRepository coverImageRepository;

    @Autowired
    private CollectionItemRepository collectionItemRepository;

    @MockitoBean
    private ExternalLookupService externalLookupService;

    @BeforeEach
    void cleanDatabase() {
        collectionItemRepository.deleteAll();
        releaseRepository.deleteAll();
    }

    @Test
    void externalCoverIsDownloadedScaledAndServedLocally() throws IOException {
        cdn.stubFor(get("/images/ok-computer.jpg").willReturn(aResponse()
                .withHeader("Content-Type", "image/png").withBody(png(1500, 1500))));
        String sourceUrl = cdn.baseUrl() + "/images/ok-computer.jpg";
        UUID id = release(MetadataSource.DISCOGS, sourceUrl).getId();

        assertThat(coverCacheService.cache(id)).isTrue();

        Release cached = releaseRepository.findById(id).orElseThrow();
        assertThat(cached.getCoverSourceUrl()).isEqualTo(sourceUrl);
        assertThat(cached.getCoverImageUrl()).startsWith("/api/covers/");
        assertThat(cached.getCoverThumbnailUrl()).isEqualTo(cached.getCoverImageUrl() + "/thumbnail");
        UUID coverId = UUID.fromString(cached.getCoverImageUrl().substring("/api/covers/".length()));
        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(coverService.get(coverId, true).data()));
        assertThat(thumbnail.getWidth()).isEqualTo(300);

        // Al gecachet: niets meer te doen.
        assertThat(coverCacheService.cache(id)).isFalse();
    }

    @Test
    void failedDownloadIsCountedAndStopsAfterMaxAttempts() {
        cdn.stubFor(get("/images/kapot.jpg").willReturn(aResponse().withStatus(404)));
        UUID id = release(MetadataSource.MUSICBRAINZ, cdn.baseUrl() + "/images/kapot.jpg").getId();

        for (int i = 0; i < 5; i++) {
            coverCacheService.cache(id);
        }

        Release release = releaseRepository.findById(id).orElseThrow();
        assertThat(release.getCoverCacheAttempts()).isEqualTo(3);
        assertThat(release.getCoverImageUrl()).isEqualTo(cdn.baseUrl() + "/images/kapot.jpg");
        assertThat(coverImageRepository.count()).isZero();
    }

    @Test
    void responseThatIsNotAnImageIsRejected() {
        cdn.stubFor(get("/images/html").willReturn(aResponse()
                .withHeader("Content-Type", "text/html").withBody("<html>Not found</html>")));
        UUID id = release(MetadataSource.DISCOGS, cdn.baseUrl() + "/images/html").getId();

        assertThat(coverCacheService.cache(id)).isFalse();
        assertThat(releaseRepository.findById(id).orElseThrow().getCoverCacheAttempts()).isEqualTo(1);
    }

    @Test
    void manualReleasesAndReleasesWithoutCoverAreSkipped() {
        UUID manual = release(MetadataSource.MANUAL, cdn.baseUrl() + "/images/x.jpg").getId();
        UUID withoutCover = release(MetadataSource.DISCOGS, null).getId();

        assertThat(coverCacheService.cache(manual)).isFalse();
        assertThat(coverCacheService.cache(withoutCover)).isFalse();
        assertThat(cdn.getAllServeEvents()).isEmpty();
    }

    @Test
    void backfillFindsOnlyUncachedExternalCovers() {
        UUID uncached = release(MetadataSource.DISCOGS, "https://i.discogs.com/a.jpg").getId();
        release(MetadataSource.DISCOGS, "/api/covers/" + UUID.randomUUID());
        release(MetadataSource.MANUAL, "https://example.com/b.jpg");

        assertThat(releaseRepository.findIdsWithUncachedCover(3, org.springframework.data.domain.PageRequest.of(0, 10)))
                .containsExactly(uncached);
    }

    private Release release(MetadataSource source, String coverUrl) {
        Release release = new Release("Radiohead", "OK Computer", MediaType.CD, source);
        release.setCoverImageUrl(coverUrl);
        release.setCoverThumbnailUrl(coverUrl);
        if (source == MetadataSource.DISCOGS) {
            release.setDiscogsId(UUID.randomUUID().toString().substring(0, 12));
        }
        return releaseRepository.save(release);
    }

    private static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
