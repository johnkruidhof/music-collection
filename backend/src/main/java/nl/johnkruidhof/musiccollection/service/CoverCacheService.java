package nl.johnkruidhof.musiccollection.service;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.client.CoverDownloader;
import nl.johnkruidhof.musiccollection.config.AppProperties;
import nl.johnkruidhof.musiccollection.entity.CoverImage;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.exception.UnsupportedImageException;
import nl.johnkruidhof.musiccollection.repository.CoverImageRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/**
 * Covers van Discogs/MusicBrainz lokaal cachen (F-21): downloaden, verkleinen (JPEG + thumbnail, net als
 * eigen uploads) en voortaan uit {@code /api/covers/{id}} serveren. De originele URL blijft bewaard in
 * {@code coverSourceUrl}. Het downloaden gebeurt buiten een transactie.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoverCacheService {

    private final ReleaseRepository releaseRepository;
    private final CoverImageRepository coverImageRepository;
    private final CoverDownloader coverDownloader;
    private final TransactionTemplate transactionTemplate;
    private final AppProperties appProperties;

    /** Na een import: op de achtergrond cachen (als ingeschakeld). */
    @Async
    public void cacheInBackground(UUID releaseId) {
        if (appProperties.covers().cacheEnabled()) {
            cache(releaseId);
        }
    }

    /** Periodiek bestaande releases bijwerken (bv. van vóór F-21, of na een tijdelijke storing). */
    @Scheduled(initialDelayString = "${app.covers.backfill-initial-delay:PT1M}",
            fixedDelayString = "${app.covers.backfill-interval:PT10M}")
    public void backfill() {
        if (!appProperties.covers().cacheEnabled()) {
            return;
        }
        List<UUID> ids = releaseRepository.findIdsWithUncachedCover(appProperties.covers().maxAttempts(),
                PageRequest.of(0, appProperties.covers().batchSize()));
        if (!ids.isEmpty()) {
            log.info("Covers cachen voor {} releases", ids.size());
            ids.forEach(this::cache);
        }
    }

    /** @return {@code true} als de cover nu lokaal staat */
    public boolean cache(UUID releaseId) {
        Release release = releaseRepository.findById(releaseId).orElse(null);
        if (release == null || !needsCaching(release)) {
            return false;
        }
        String sourceUrl = release.getCoverImageUrl();
        try {
            BufferedImage image = CoverImages.read(coverDownloader.download(sourceUrl));
            byte[] full = CoverImages.toJpeg(image, CoverImages.IMAGE_MAX_SIZE);
            byte[] thumbnail = CoverImages.toJpeg(image, CoverImages.THUMBNAIL_MAX_SIZE);
            transactionTemplate.executeWithoutResult(status -> store(releaseId, sourceUrl, full, thumbnail));
            return true;
        } catch (RestClientException | UnsupportedImageException ex) {
            log.warn("Cover van release {} niet te cachen ({}): {}", releaseId, sourceUrl, ex.getMessage());
            transactionTemplate.executeWithoutResult(status -> releaseRepository.findById(releaseId)
                    .ifPresent(r -> r.setCoverCacheAttempts(r.getCoverCacheAttempts() + 1)));
            return false;
        }
    }

    private void store(UUID releaseId, String sourceUrl, byte[] full, byte[] thumbnail) {
        Release release = releaseRepository.findById(releaseId).orElse(null);
        // Intussen verwijderd of al gecachet (bv. backfill en import tegelijk)?
        if (release == null || !sourceUrl.equals(release.getCoverImageUrl())) {
            return;
        }
        coverImageRepository.deleteByReleaseId(releaseId);
        CoverImage cover = coverImageRepository.save(
                new CoverImage(releaseId, full, thumbnail, CoverImages.CONTENT_TYPE));
        release.setCoverSourceUrl(sourceUrl);
        release.setCoverImageUrl(CoverService.COVER_URL_PREFIX + cover.getId());
        release.setCoverThumbnailUrl(CoverService.COVER_URL_PREFIX + cover.getId() + "/thumbnail");
        log.debug("Cover van release {} lokaal opgeslagen", releaseId);
    }

    private boolean needsCaching(Release release) {
        String url = release.getCoverImageUrl();
        return release.getSource() != MetadataSource.MANUAL
                && url != null
                && !url.startsWith(CoverService.COVER_URL_PREFIX)
                && release.getCoverCacheAttempts() < appProperties.covers().maxAttempts();
    }
}
