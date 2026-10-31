package nl.johnkruidhof.musiccollection.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import nl.johnkruidhof.musiccollection.client.discogs.DiscogsRelease;
import nl.johnkruidhof.musiccollection.client.discogs.DiscogsSearchResult;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;

/**
 * Discogs-antwoorden naar onze DTO's. Discogs levert veel als vrije tekst ("Artiest - Titel",
 * jaartal als string, formaat als lijst namen), daarom de {@code @Named}-hulpmethodes.
 */
@Mapper
public abstract class DiscogsMapper {

    /** Discogs zet "(2)", "(3)" ... achter namen om gelijknamige artiesten te onderscheiden. */
    private static final Pattern ARTIST_SUFFIX = Pattern.compile("\\s*\\(\\d+\\)$");
    /** "1-3", "CD2-5", "2.04": schijfnummer vóór het scheidingsteken. */
    private static final Pattern DISC_TRACK_POSITION = Pattern.compile("^[A-Za-z]*(\\d+)[-.]\\d+");
    /** "A1", "B", "C12", "D1a" (deeltrack): vinylkant; twee kanten per plaat. */
    private static final Pattern VINYL_SIDE_POSITION = Pattern.compile("^([A-Z])\\d*[a-z]?$");
    private static final Pattern DURATION = Pattern.compile("^(?:(\\d+):)?(\\d+):(\\d{2})$");

    @Mapping(target = "source", constant = "DISCOGS")
    @Mapping(target = "externalId", source = "id")
    @Mapping(target = "artist", source = "title", qualifiedByName = "artistFromSearchTitle")
    @Mapping(target = "title", source = "title", qualifiedByName = "titleFromSearchTitle")
    @Mapping(target = "releaseYear", source = "year", qualifiedByName = "parseYear")
    @Mapping(target = "mediaType", source = "format", qualifiedByName = "mediaTypeFromNames")
    @Mapping(target = "formatDetails", source = "format", qualifiedByName = "joinNames")
    @Mapping(target = "label", source = "label", qualifiedByName = "firstOf")
    @Mapping(target = "catalogNumber", source = "catno", qualifiedByName = "blankToNull")
    @Mapping(target = "country", source = "country", qualifiedByName = "blankToNull")
    @Mapping(target = "barcode", source = "barcode", qualifiedByName = "firstBarcode")
    @Mapping(target = "coverThumbnailUrl", source = "thumb", qualifiedByName = "imageUrl")
    @Mapping(target = "ownedCount", ignore = true)
    @Mapping(target = "onWishlist", ignore = true)
    public abstract ReleaseCandidateDto toCandidate(DiscogsSearchResult result);

    public abstract List<ReleaseCandidateDto> toCandidates(List<DiscogsSearchResult> results);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "source", constant = "DISCOGS")
    @Mapping(target = "discogsId", source = "id")
    @Mapping(target = "musicbrainzId", ignore = true)
    @Mapping(target = "barcode", source = "identifiers", qualifiedByName = "barcodeFromIdentifiers")
    @Mapping(target = "artist", source = "artists", qualifiedByName = "artistNames")
    @Mapping(target = "releaseYear", source = "year", qualifiedByName = "yearOrNull")
    @Mapping(target = "mediaType", source = "formats", qualifiedByName = "mediaTypeFromFormats")
    @Mapping(target = "formatDetails", source = "formats", qualifiedByName = "formatDetails")
    @Mapping(target = "label", source = "labels", qualifiedByName = "firstLabelName")
    @Mapping(target = "catalogNumber", source = "labels", qualifiedByName = "firstCatalogNumber")
    @Mapping(target = "country", source = "country", qualifiedByName = "blankToNull")
    @Mapping(target = "coverImageUrl", source = "images", qualifiedByName = "coverImage")
    @Mapping(target = "coverThumbnailUrl", source = "images", qualifiedByName = "coverThumbnail")
    @Mapping(target = "tracks", source = "tracklist", qualifiedByName = "tracks")
    public abstract ReleaseDto toReleaseDto(DiscogsRelease release);

    /** Waarde voor de {@code format}-parameter van de Discogs-zoek-API; {@code null} = niet filteren. */
    @ValueMapping(source = "VINYL", target = "Vinyl")
    @ValueMapping(source = "CD", target = "CD")
    @ValueMapping(source = "DVD", target = "DVD")
    @ValueMapping(source = "BLU_RAY", target = "Blu-ray")
    @ValueMapping(source = "CASSETTE", target = "Cassette")
    @ValueMapping(source = "OTHER", target = MappingConstants.NULL)
    public abstract String toDiscogsFormat(MediaType mediaType);

    // --- zoekresultaten ---

    @Named("artistFromSearchTitle")
    String artistFromSearchTitle(String title) {
        int sep = title == null ? -1 : title.indexOf(" - ");
        return sep < 0 ? null : cleanArtistName(title.substring(0, sep));
    }

    @Named("titleFromSearchTitle")
    String titleFromSearchTitle(String title) {
        int sep = title == null ? -1 : title.indexOf(" - ");
        return blankToNull(sep < 0 ? title : title.substring(sep + 3));
    }

    @Named("parseYear")
    Integer parseYear(String year) {
        if (year == null || !year.strip().matches("\\d{4}")) {
            return null;
        }
        return yearOrNull(Integer.parseInt(year.strip()));
    }

    @Named("mediaTypeFromNames")
    MediaType mediaTypeFromNames(List<String> formatNames) {
        if (formatNames == null) {
            return MediaType.OTHER;
        }
        return formatNames.stream()
                .map(DiscogsMapper::mediaTypeOf)
                .filter(type -> type != MediaType.OTHER)
                .findFirst()
                .orElse(MediaType.OTHER);
    }

    @Named("joinNames")
    String joinNames(List<String> names) {
        return names == null || names.isEmpty() ? null : String.join(", ", names);
    }

    @Named("firstOf")
    String firstOf(List<String> values) {
        return values == null ? null : values.stream().map(this::blankToNull).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Named("firstBarcode")
    String firstBarcode(List<String> barcodes) {
        return barcodes == null ? null : barcodes.stream().map(DiscogsMapper::digitsOnlyBarcode)
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Named("imageUrl")
    String imageUrl(String url) {
        // Discogs geeft een lege string of een spacer.gif als er geen afbeelding is.
        return url == null || url.isBlank() || url.endsWith("spacer.gif") ? null : url;
    }

    // --- release-details ---

    @Named("yearOrNull")
    Integer yearOrNull(Integer year) {
        // Discogs gebruikt 0 voor "onbekend".
        return year == null || year < 1000 ? null : year;
    }

    @Named("barcodeFromIdentifiers")
    String barcodeFromIdentifiers(List<DiscogsRelease.Identifier> identifiers) {
        return identifiers == null ? null : identifiers.stream()
                .filter(id -> "Barcode".equalsIgnoreCase(id.type()))
                .map(id -> digitsOnlyBarcode(id.value()))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Named("artistNames")
    String artistNames(List<DiscogsRelease.Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            return null;
        }
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < artists.size(); i++) {
            DiscogsRelease.Artist artist = artists.get(i);
            names.append(cleanArtistName(artist.name()));
            if (i < artists.size() - 1) {
                String join = artist.join() == null ? "" : artist.join().strip();
                names.append(join.isEmpty() || join.equals(",") ? ", " : " " + join + " ");
            }
        }
        return names.toString();
    }

    @Named("mediaTypeFromFormats")
    MediaType mediaTypeFromFormats(List<DiscogsRelease.Format> formats) {
        return formats == null ? MediaType.OTHER
                : mediaTypeFromNames(formats.stream().map(DiscogsRelease.Format::name).toList());
    }

    /** Bv. "2×Vinyl, LP, Album" of "CD, Album + DVD, NTSC". */
    @Named("formatDetails")
    String formatDetails(List<DiscogsRelease.Format> formats) {
        if (formats == null || formats.isEmpty()) {
            return null;
        }
        return formats.stream().map(format -> {
            List<String> parts = new ArrayList<>();
            String qty = format.qty() == null ? "" : format.qty().strip();
            parts.add((qty.isEmpty() || qty.equals("1") ? "" : qty + "×") + format.name());
            if (format.descriptions() != null) {
                parts.addAll(format.descriptions());
            }
            if (format.text() != null && !format.text().isBlank()) {
                parts.add(format.text().strip());
            }
            return String.join(", ", parts);
        }).collect(Collectors.joining(" + "));
    }

    @Named("firstLabelName")
    String firstLabelName(List<DiscogsRelease.Label> labels) {
        return labels == null || labels.isEmpty() ? null : cleanArtistName(labels.getFirst().name());
    }

    @Named("firstCatalogNumber")
    String firstCatalogNumber(List<DiscogsRelease.Label> labels) {
        // "none" betekent bij Discogs: geen catalogusnummer.
        return labels == null || labels.isEmpty() || "none".equalsIgnoreCase(labels.getFirst().catno())
                ? null : blankToNull(labels.getFirst().catno());
    }

    @Named("coverImage")
    String coverImage(List<DiscogsRelease.Image> images) {
        DiscogsRelease.Image image = primaryImage(images);
        return image == null ? null : imageUrl(image.uri());
    }

    @Named("coverThumbnail")
    String coverThumbnail(List<DiscogsRelease.Image> images) {
        DiscogsRelease.Image image = primaryImage(images);
        return image == null ? null : imageUrl(image.uri150());
    }

    /**
     * Platte tracklijst: tussenkopjes ("heading") vallen weg, een "index"-track (bv. een suite)
     * wordt vervangen door zijn deeltracks. {@code sortOrder} loopt door vanaf 1.
     */
    @Named("tracks")
    List<TrackDto> tracks(List<DiscogsRelease.Track> tracklist) {
        List<TrackDto> tracks = new ArrayList<>();
        if (tracklist == null) {
            return tracks;
        }
        for (DiscogsRelease.Track track : tracklist) {
            if ("heading".equals(track.type())) {
                continue;
            }
            if ("index".equals(track.type()) && track.subTracks() != null && !track.subTracks().isEmpty()) {
                track.subTracks().forEach(sub -> addTrack(tracks, sub));
            } else {
                addTrack(tracks, track);
            }
        }
        return tracks;
    }

    @Named("blankToNull")
    String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private void addTrack(List<TrackDto> tracks, DiscogsRelease.Track track) {
        String title = blankToNull(track.title());
        if (title == null) {
            return;
        }
        String position = blankToNull(track.position());
        tracks.add(new TrackDto(discNumber(position), position, tracks.size() + 1, title,
                durationSeconds(track.duration()), artistNames(track.artists())));
    }

    /** Schijfnummer uit de positie: "2-5" → 2, "C3" → 2 (kant C = tweede plaat), anders 1. */
    static int discNumber(String position) {
        if (position == null) {
            return 1;
        }
        Matcher discTrack = DISC_TRACK_POSITION.matcher(position);
        if (discTrack.find()) {
            return Math.max(1, Integer.parseInt(discTrack.group(1)));
        }
        Matcher side = VINYL_SIDE_POSITION.matcher(position);
        if (side.matches()) {
            return (side.group(1).charAt(0) - 'A') / 2 + 1;
        }
        return 1;
    }

    /** "4:44" → 284, "1:02:03" → 3723; leeg of onleesbaar → {@code null}. */
    static Integer durationSeconds(String duration) {
        if (duration == null) {
            return null;
        }
        Matcher m = DURATION.matcher(duration.strip());
        if (!m.matches()) {
            return null;
        }
        int hours = m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
        return hours * 3600 + Integer.parseInt(m.group(2)) * 60 + Integer.parseInt(m.group(3));
    }

    static MediaType mediaTypeOf(String formatName) {
        if (formatName == null) {
            return MediaType.OTHER;
        }
        return switch (formatName.strip()) {
            case "Vinyl", "Lathe Cut", "Flexi-disc", "Shellac" -> MediaType.VINYL;
            case "CD", "CDr", "SACD", "Hybrid", "CDV" -> MediaType.CD;
            case "DVD", "DVDr", "HD DVD" -> MediaType.DVD;
            case "Blu-ray", "Blu-ray-R" -> MediaType.BLU_RAY;
            case "Cassette", "Microcassette" -> MediaType.CASSETTE;
            default -> MediaType.OTHER;
        };
    }

    private static String cleanArtistName(String name) {
        if (name == null) {
            return null;
        }
        // "*" markeert bij Discogs een naamvariant (ANV); voor ons niet relevant.
        String cleaned = ARTIST_SUFFIX.matcher(name.strip()).replaceAll("");
        return cleaned.endsWith("*") ? cleaned.substring(0, cleaned.length() - 1) : cleaned;
    }

    /** Alleen cijfers; Discogs-barcodes bevatten vaak spaties ("7 24385 52291 2"). Geen cijfers → {@code null}. */
    private static String digitsOnlyBarcode(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[\\s-]", "");
        return digits.matches("\\d{6,14}") ? digits : null;
    }

    private static DiscogsRelease.Image primaryImage(List<DiscogsRelease.Image> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream().filter(img -> "primary".equals(img.type())).findFirst().orElse(images.getFirst());
    }
}
