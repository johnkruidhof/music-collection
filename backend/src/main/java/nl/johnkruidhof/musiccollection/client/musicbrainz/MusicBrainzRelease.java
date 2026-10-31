package nl.johnkruidhof.musiccollection.client.musicbrainz;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Een release uit de zoek-API of uit {@code GET /release/{mbid}}; alleen de velden die we gebruiken.
 * {@code date} is "1997", "1997-06" of "1997-06-16"; {@code country} een ISO-code (ook "XE" = Europa).
 */
public record MusicBrainzRelease(
        String id,
        String title,
        String date,
        String country,
        String barcode,
        @JsonProperty("artist-credit") List<ArtistCredit> artistCredit,
        @JsonProperty("label-info") List<LabelInfo> labelInfo,
        List<Medium> media,
        @JsonProperty("cover-art-archive") CoverArtStatus coverArtArchive) {

    /** {@code name} is de naam zoals vermeld; {@code joinphrase} verbindt met de volgende (" & ", " feat. "). */
    public record ArtistCredit(String name, String joinphrase) {
    }

    public record LabelInfo(@JsonProperty("catalog-number") String catalogNumber, Label label) {
    }

    public record Label(String name) {
    }

    /** {@code position} is het schijfnummer; {@code format} bv. "CD", "12\" Vinyl", "DVD-Video". */
    public record Medium(Integer position, String format, List<Track> tracks) {
    }

    /** {@code number} is de positie zoals op de hoes ("1", "A1"); {@code length} in milliseconden. */
    public record Track(
            String number,
            String title,
            Long length,
            @JsonProperty("artist-credit") List<ArtistCredit> artistCredit) {
    }

    /** Alleen in {@code GET /release/{mbid}}: of het Cover Art Archive een voorkant heeft. */
    public record CoverArtStatus(boolean front) {
    }
}
