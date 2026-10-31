package nl.johnkruidhof.musiccollection.dto.importexport;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Voortgang van een import (F-22). {@code total} = aantal regels; {@code imported} = toegevoegde
 * exemplaren; {@code skipped} = stond al in de collectie; {@code failed} = niet gevonden of fout.
 */
public record ImportJobDto(
        UUID id,
        Status status,
        int total,
        int processed,
        int imported,
        int skipped,
        int failed,
        List<String> errors,
        Instant startedAt,
        Instant finishedAt) {

    public enum Status {
        RUNNING,
        FINISHED,
        FAILED
    }
}
