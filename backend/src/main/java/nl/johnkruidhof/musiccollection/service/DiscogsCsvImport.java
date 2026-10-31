package nl.johnkruidhof.musiccollection.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;

/**
 * Leest de collectie-export van Discogs (Collection → Export). Kolommen o.a. {@code Artist},
 * {@code Title}, {@code release_id}, {@code Collection Media Condition}, {@code Collection Sleeve Condition},
 * {@code Collection Notes}; eventuele eigen velden worden genegeerd.
 */
final class DiscogsCsvImport {

    record Row(int line, String releaseId, String artist, String title, MediaCondition conditionMedia,
            MediaCondition conditionSleeve, String notes) {

        String description() {
            return "regel " + line + " (" + artist + " – " + title + ")";
        }
    }

    /** Discogs-graderingen → Goldmine-enum; "Generic", "No Cover", "Not Graded" → onbekend. */
    private static final Map<String, MediaCondition> CONDITIONS = Map.of(
            "M", MediaCondition.MINT,
            "NM", MediaCondition.NEAR_MINT,
            "VG+", MediaCondition.VERY_GOOD_PLUS,
            "VG", MediaCondition.VERY_GOOD,
            "G+", MediaCondition.GOOD,
            "G", MediaCondition.GOOD,
            "F", MediaCondition.POOR,
            "P", MediaCondition.POOR);

    private DiscogsCsvImport() {
    }

    static List<Row> parse(String csv) {
        List<List<String>> rows = Csv.parse(csv, ',');
        if (rows.isEmpty()) {
            throw new InvalidRequestException("Het bestand is leeg.");
        }
        List<String> header = rows.getFirst().stream().map(String::strip).toList();
        int releaseId = header.indexOf("release_id");
        if (releaseId < 0) {
            throw new InvalidRequestException(
                    "Dit lijkt geen collectie-export van Discogs: de kolom 'release_id' ontbreekt.");
        }
        int artist = header.indexOf("Artist");
        int title = header.indexOf("Title");
        int media = header.indexOf("Collection Media Condition");
        int sleeve = header.indexOf("Collection Sleeve Condition");
        int notes = header.indexOf("Collection Notes");

        List<Row> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            result.add(new Row(i + 1, cell(row, releaseId), cell(row, artist), cell(row, title),
                    condition(cell(row, media)), condition(cell(row, sleeve)), blankToNull(cell(row, notes))));
        }
        return result;
    }

    /** "Near Mint (NM or M-)" → NEAR_MINT, "Very Good Plus (VG+)" → VERY_GOOD_PLUS. */
    static MediaCondition condition(String discogs) {
        if (discogs == null) {
            return null;
        }
        int open = discogs.lastIndexOf('(');
        int close = discogs.lastIndexOf(')');
        if (open < 0 || close < open) {
            return null;
        }
        String code = discogs.substring(open + 1, close).strip().toUpperCase(Locale.ROOT).split("\\s+")[0];
        return CONDITIONS.get(code);
    }

    private static String cell(List<String> row, int index) {
        return index >= 0 && index < row.size() ? row.get(index).strip() : "";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
