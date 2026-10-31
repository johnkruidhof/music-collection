package nl.johnkruidhof.musiccollection.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.dto.admin.AdminReleaseDto;
import nl.johnkruidhof.musiccollection.dto.admin.AdminReleasePageDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.repository.AdminReleaseRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/**
 * Catalogusbeheer (F-27): de gedeelde catalogus doorzoeken, ongebruikte releases opruimen en dubbele
 * releases samenvoegen. Toegang via {@code /api/admin/**} (rol {@code admin}, zie SecurityConfig).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    static final int MAX_PAGE_SIZE = 100;
    private static final String NO_MATCH = "#";

    private final AdminReleaseRepository adminReleaseRepository;
    private final ReleaseRepository releaseRepository;

    @Transactional(readOnly = true)
    public AdminReleasePageDto search(String q, MetadataSource source, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("page moet ≥ 0 zijn en size tussen 1 en " + MAX_PAGE_SIZE + ".");
        }
        String text = q == null ? "" : q.strip();
        String pattern = "%" + text.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_") + "%";
        Set<MetadataSource> sources = source == null ? EnumSet.allOf(MetadataSource.class) : EnumSet.of(source);
        // Zonder zoekterm een "exact"-waarde die nooit een barcode of id is (geen NUL: dat weigert Postgres).
        Page<AdminReleaseDto> result = adminReleaseRepository.search(sources, pattern, text.isEmpty() ? NO_MATCH : text,
                PageRequest.of(page, size, Sort.by("artist", "title", "releaseYear")));
        return new AdminReleasePageDto(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), adminReleaseRepository.findUnusedIds().size());
    }

    /** Alleen een release die nergens meer in staat (geen collectie-items, geen wensen). */
    @Transactional
    public void delete(UUID releaseId) {
        Release release = find(releaseId);
        if (adminReleaseRepository.countCollectionItems(releaseId) > 0
                || adminReleaseRepository.countWishlistItems(releaseId) > 0) {
            throw new InvalidRequestException("Deze release staat nog in een collectie of op een verlanglijst; "
                    + "voeg hem samen met een andere release in plaats van te verwijderen.");
        }
        releaseRepository.delete(release);
        log.info("Release {} ({} - {}) verwijderd door beheerder", releaseId, release.getArtist(), release.getTitle());
    }

    /** @return aantal verwijderde releases */
    @Transactional
    public int deleteUnused() {
        List<UUID> ids = adminReleaseRepository.findUnusedIds();
        releaseRepository.deleteAllById(ids);
        log.info("{} ongebruikte releases opgeruimd door beheerder", ids.size());
        return ids.size();
    }

    /**
     * Voegt {@code sourceId} samen met {@code targetId}: collectie-items en wensen verhuizen naar het doel
     * (een wens die bij die gebruiker al op het doel staat vervalt), daarna wordt de bron verwijderd.
     */
    @Transactional
    public AdminReleaseDto merge(UUID sourceId, UUID targetId) {
        if (sourceId.equals(targetId)) {
            throw new InvalidRequestException("Een release kan niet met zichzelf worden samengevoegd.");
        }
        Release source = find(sourceId);
        Release target = find(targetId);
        int items = adminReleaseRepository.moveCollectionItems(sourceId, targetId);
        int duplicateWishes = adminReleaseRepository.deleteDuplicateWishes(sourceId, targetId);
        int wishes = adminReleaseRepository.moveWishes(sourceId, targetId);
        releaseRepository.deleteById(sourceId);
        log.info("Release {} ({} - {}) samengevoegd met {}: {} items, {} wensen verplaatst, {} dubbele wensen vervallen",
                sourceId, source.getArtist(), source.getTitle(), targetId, items, wishes, duplicateWishes);
        return adminReleaseRepository.findDtoById(target.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Release", targetId));
    }

    private Release find(UUID id) {
        return releaseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Release", id));
    }
}
