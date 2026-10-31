package nl.johnkruidhof.musiccollection.service;

import java.awt.image.BufferedImage;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;
import nl.johnkruidhof.musiccollection.entity.CoverImage;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.mapper.CollectionItemMapper;
import nl.johnkruidhof.musiccollection.repository.CoverImageRepository;

/**
 * Eigen covers voor handmatige releases. Releases van Discogs/MusicBrainz staan in de gedeelde
 * catalogus en houden hun eigen cover. De afbeelding wordt (verkleind) in de database bewaard en is
 * zonder login op te halen via {@code /api/covers/{id}}: een {@code <img>} kan geen token meesturen,
 * het willekeurige UUID maakt de URL onraadbaar.
 */
@Service
@RequiredArgsConstructor
public class CoverService {

    public static final String COVER_URL_PREFIX = "/api/covers/";

    private final CollectionService collectionService;
    private final CoverImageRepository coverImageRepository;
    private final CollectionItemMapper collectionItemMapper;

    public record CoverData(byte[] data, String contentType) {
    }

    @Transactional
    public CollectionItemDto upload(Jwt jwt, UUID itemId, byte[] data) {
        Release release = manualRelease(collectionService.findOwned(jwt, itemId));
        if (data == null || data.length == 0) {
            throw new InvalidRequestException("Er is geen afbeelding meegestuurd.");
        }
        BufferedImage image = CoverImages.read(data);
        coverImageRepository.deleteByReleaseId(release.getId());
        CoverImage cover = coverImageRepository.saveAndFlush(new CoverImage(release.getId(),
                CoverImages.toJpeg(image, CoverImages.IMAGE_MAX_SIZE),
                CoverImages.toJpeg(image, CoverImages.THUMBNAIL_MAX_SIZE),
                CoverImages.CONTENT_TYPE));
        release.setCoverImageUrl(COVER_URL_PREFIX + cover.getId());
        release.setCoverThumbnailUrl(COVER_URL_PREFIX + cover.getId() + "/thumbnail");
        return collectionItemMapper.toDto(collectionService.findOwned(jwt, itemId));
    }

    @Transactional
    public CollectionItemDto delete(Jwt jwt, UUID itemId) {
        Release release = manualRelease(collectionService.findOwned(jwt, itemId));
        coverImageRepository.deleteByReleaseId(release.getId());
        release.setCoverImageUrl(null);
        release.setCoverThumbnailUrl(null);
        return collectionItemMapper.toDto(collectionService.findOwned(jwt, itemId));
    }

    @Transactional(readOnly = true)
    public CoverData get(UUID coverId, boolean thumbnail) {
        CoverImage cover = coverImageRepository.findById(coverId)
                .orElseThrow(() -> new ResourceNotFoundException("Cover", coverId));
        return new CoverData(thumbnail ? cover.getThumbnail() : cover.getImage(), cover.getContentType());
    }

    private static Release manualRelease(CollectionItem item) {
        Release release = item.getRelease();
        if (release.getSource() != MetadataSource.MANUAL) {
            throw new InvalidRequestException("Alleen bij een handmatig ingevoerde release kun je een eigen cover instellen.");
        }
        return release;
    }
}
