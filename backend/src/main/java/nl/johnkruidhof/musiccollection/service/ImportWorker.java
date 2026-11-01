package nl.johnkruidhof.musiccollection.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.dto.importexport.ImportJobDto;
import nl.johnkruidhof.musiccollection.entity.AppUser;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.repository.AppUserRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/**
 * Verwerkt een Discogs-import op de achtergrond. Per release wordt het <b>tekort</b> aan exemplaren
 * toegevoegd (aantal regels in de CSV minus wat al in de collectie staat), zodat dezelfde export twee keer
 * importeren geen dubbele items oplevert en meerdere exemplaren behouden blijven.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ImportWorker {

    private final CatalogService catalogService;
    private final ReleaseRepository releaseRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final AppUserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final GenreService genreService;

    @Async
    public void run(ImportService.Job job, List<DiscogsCsvImport.Row> rows) {
        try {
            Map<String, List<DiscogsCsvImport.Row>> byRelease = new LinkedHashMap<>();
            for (DiscogsCsvImport.Row row : rows) {
                if (!row.releaseId().matches("\\d{1,12}")) {
                    job.failed++;
                    job.processed++;
                    job.error(row.description() + ": geen geldig release_id");
                } else {
                    byRelease.computeIfAbsent(row.releaseId(), id -> new java.util.ArrayList<>()).add(row);
                }
            }
            byRelease.forEach((releaseId, copies) -> importRelease(job, releaseId, copies));
            job.status = ImportJobDto.Status.FINISHED;
        } catch (RuntimeException ex) {
            log.error("Import {} afgebroken", job.id, ex);
            job.error("Import afgebroken: " + ex.getMessage());
            job.status = ImportJobDto.Status.FAILED;
        } finally {
            job.finishedAt = Instant.now();
            log.info("Import {} klaar: {} toegevoegd, {} overgeslagen, {} mislukt van {}", job.id, job.imported,
                    job.skipped, job.failed, job.total);
        }
    }

    private void importRelease(ImportService.Job job, String discogsId, List<DiscogsCsvImport.Row> copies) {
        UUID releaseId;
        try {
            releaseId = catalogService.getOrImport(MetadataSource.DISCOGS, discogsId);
        } catch (ResourceNotFoundException | ExternalLookupException ex) {
            job.failed += copies.size();
            job.processed += copies.size();
            job.error(copies.getFirst().description() + (ex instanceof ResourceNotFoundException
                    ? ": niet gevonden bij Discogs"
                    : ": Discogs was niet bereikbaar, probeer later opnieuw"));
            return;
        }
        int added = transactionTemplate.execute(status -> addMissingCopies(job.userId, releaseId, copies));
        job.imported += added;
        job.skipped += copies.size() - added;
        job.processed += copies.size();
    }

    private int addMissingCopies(UUID userId, UUID releaseId, List<DiscogsCsvImport.Row> copies) {
        AppUser user = userRepository.getReferenceById(userId);
        Release release = releaseRepository.getReferenceById(releaseId);
        long existing = collectionItemRepository.countByUserIdAndReleaseId(userId, releaseId);
        int added = 0;
        for (int i = (int) existing; i < copies.size(); i++) {
            DiscogsCsvImport.Row row = copies.get(i);
            CollectionItem item = new CollectionItem(user, release);
            item.setConditionMedia(row.conditionMedia());
            item.setConditionSleeve(row.conditionSleeve());
            item.setNotes(row.notes() == null ? null : row.notes().substring(0, Math.min(4000, row.notes().length())));
            item.setGenre(genreService.suggest(userId, release.getArtist()).orElse(null));
            collectionItemRepository.save(item);
            added++;
        }
        return added;
    }
}
