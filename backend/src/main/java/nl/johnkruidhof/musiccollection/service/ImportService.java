package nl.johnkruidhof.musiccollection.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.importexport.ImportJobDto;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;

/**
 * Import van een Discogs-collectie-export als achtergrondtaak (F-22). De CSV wordt direct gecontroleerd;
 * het ophalen bij Discogs (1 request/s) loopt via {@link ImportWorker}. Taken staan in het geheugen: na een
 * herstart is de voortgang weg, maar opnieuw importeren is veilig (zie {@link ImportWorker}).
 */
@Service
@RequiredArgsConstructor
public class ImportService {

    static final int MAX_ROWS = 10_000;
    static final int MAX_ERRORS = 100;

    private final UserService userService;
    private final ImportWorker importWorker;
    private final Map<UUID, Job> jobs = new ConcurrentHashMap<>();

    /** Voortgang van één import; gedeeld tussen de worker-thread en de API. */
    static final class Job {
        final UUID id = UUID.randomUUID();
        final UUID userId;
        final int total;
        final Instant startedAt = Instant.now();
        volatile ImportJobDto.Status status = ImportJobDto.Status.RUNNING;
        volatile int processed;
        volatile int imported;
        volatile int skipped;
        volatile int failed;
        volatile Instant finishedAt;
        final List<String> errors = Collections.synchronizedList(new ArrayList<>());

        Job(UUID userId, int total) {
            this.userId = userId;
            this.total = total;
        }

        void error(String message) {
            if (errors.size() < MAX_ERRORS) {
                errors.add(message);
            }
        }

        ImportJobDto toDto() {
            return new ImportJobDto(id, status, total, processed, imported, skipped, failed, List.copyOf(errors),
                    startedAt, finishedAt);
        }
    }

    public ImportJobDto startDiscogsImport(Jwt jwt, byte[] csv) {
        UUID userId = userService.getOrCreate(jwt).getId();
        List<DiscogsCsvImport.Row> rows = DiscogsCsvImport.parse(new String(csv, StandardCharsets.UTF_8));
        if (rows.isEmpty()) {
            throw new InvalidRequestException("Het bestand bevat geen releases.");
        }
        if (rows.size() > MAX_ROWS) {
            throw new InvalidRequestException("Maximaal " + MAX_ROWS + " regels per import.");
        }
        Job job;
        synchronized (jobs) {
            boolean running = jobs.values().stream()
                    .anyMatch(j -> j.userId.equals(userId) && j.status == ImportJobDto.Status.RUNNING);
            if (running) {
                throw new InvalidRequestException("Er loopt al een import; wacht tot die klaar is.");
            }
            job = new Job(userId, rows.size());
            jobs.put(job.id, job);
        }
        importWorker.run(job, rows);
        return job.toDto();
    }

    public ImportJobDto get(Jwt jwt, UUID jobId) {
        UUID userId = userService.getOrCreate(jwt).getId();
        Job job = jobs.get(jobId);
        if (job == null || !job.userId.equals(userId)) {
            throw new ResourceNotFoundException("Import", jobId);
        }
        return job.toDto();
    }

    /** De meest recente import van de gebruiker (lopend of afgerond), of {@code null}. */
    public ImportJobDto latest(Jwt jwt) {
        UUID userId = userService.getOrCreate(jwt).getId();
        return jobs.values().stream()
                .filter(j -> j.userId.equals(userId))
                .max((a, b) -> a.startedAt.compareTo(b.startedAt))
                .map(Job::toDto)
                .orElse(null);
    }
}
