package nl.johnkruidhof.musiccollection.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import nl.johnkruidhof.musiccollection.client.musicbrainz.CoverArt;
import nl.johnkruidhof.musiccollection.client.musicbrainz.MusicBrainzRelease;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;

/** MusicBrainz-releases (en Cover Art Archive-URL's) naar onze DTO's. */
@Mapper
public abstract class MusicBrainzMapper {

    /**
     * Zoekresultaten zeggen niet of er een cover is; de thumbnail-URL van het Cover Art Archive
     * wordt daarom opgebouwd uit het MBID en kan een 404 geven (frontend toont dan een placeholder).
     */
    @Mapping(target = "source", constant = "MUSICBRAINZ")
    @Mapping(target = "externalId", source = "id")
    @Mapping(target = "artist", source = "artistCredit", qualifiedByName = "artistCredit")
    @Mapping(target = "title", source = "title", qualifiedByName = "blankToNull")
    @Mapping(target = "releaseYear", source = "date", qualifiedByName = "yearFromDate")
    @Mapping(target = "mediaType", source = "media", qualifiedByName = "mediaType")
    @Mapping(target = "formatDetails", source = "media", qualifiedByName = "formatDetails")
    @Mapping(target = "label", source = "labelInfo", qualifiedByName = "firstLabelName")
    @Mapping(target = "catalogNumber", source = "labelInfo", qualifiedByName = "firstCatalogNumber")
    @Mapping(target = "country", source = "country", qualifiedByName = "blankToNull")
    @Mapping(target = "barcode", source = "barcode", qualifiedByName = "blankToNull")
    @Mapping(target = "coverThumbnailUrl", source = "id", qualifiedByName = "frontThumbnailUrl")
    @Mapping(target = "ownedCount", ignore = true)
    @Mapping(target = "onWishlist", ignore = true)
    public abstract ReleaseCandidateDto toCandidate(MusicBrainzRelease release, @Context String coverArtBaseUrl);

    public abstract List<ReleaseCandidateDto> toCandidates(List<MusicBrainzRelease> releases,
            @Context String coverArtBaseUrl);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "source", constant = "MUSICBRAINZ")
    @Mapping(target = "musicbrainzId", source = "release.id")
    @Mapping(target = "discogsId", ignore = true)
    @Mapping(target = "barcode", source = "release.barcode", qualifiedByName = "blankToNull")
    @Mapping(target = "artist", source = "release.artistCredit", qualifiedByName = "artistCredit")
    @Mapping(target = "title", source = "release.title", qualifiedByName = "blankToNull")
    @Mapping(target = "releaseYear", source = "release.date", qualifiedByName = "yearFromDate")
    @Mapping(target = "mediaType", source = "release.media", qualifiedByName = "mediaType")
    @Mapping(target = "formatDetails", source = "release.media", qualifiedByName = "formatDetails")
    @Mapping(target = "label", source = "release.labelInfo", qualifiedByName = "firstLabelName")
    @Mapping(target = "catalogNumber", source = "release.labelInfo", qualifiedByName = "firstCatalogNumber")
    @Mapping(target = "country", source = "release.country", qualifiedByName = "blankToNull")
    @Mapping(target = "coverImageUrl", source = "coverArt.imageUrl")
    @Mapping(target = "coverThumbnailUrl", source = "coverArt.thumbnailUrl")
    @Mapping(target = "tracks", source = "release", qualifiedByName = "tracks")
    public abstract ReleaseDto toReleaseDto(MusicBrainzRelease release, CoverArt coverArt);

    @Named("artistCredit")
    String artistCredit(List<MusicBrainzRelease.ArtistCredit> credits) {
        if (credits == null || credits.isEmpty()) {
            return null;
        }
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < credits.size(); i++) {
            MusicBrainzRelease.ArtistCredit credit = credits.get(i);
            names.append(credit.name());
            if (i < credits.size() - 1 && credit.joinphrase() != null) {
                names.append(credit.joinphrase());
            }
        }
        return blankToNull(names.toString());
    }

    @Named("yearFromDate")
    Integer yearFromDate(String date) {
        return date != null && date.matches("\\d{4}(-.*)?") ? Integer.valueOf(date.substring(0, 4)) : null;
    }

    /** Type van het eerste medium met een herkend formaat (bv. CD + DVD → CD). */
    @Named("mediaType")
    MediaType mediaType(List<MusicBrainzRelease.Medium> media) {
        if (media == null) {
            return MediaType.OTHER;
        }
        return media.stream()
                .map(medium -> mediaTypeOf(medium.format()))
                .filter(type -> type != MediaType.OTHER)
                .findFirst()
                .orElse(MediaType.OTHER);
    }

    /** Opeenvolgende gelijke media samengevoegd: "2×12\" Vinyl", "2×CD + DVD-Video". */
    @Named("formatDetails")
    String formatDetails(List<MusicBrainzRelease.Medium> media) {
        if (media == null) {
            return null;
        }
        List<String> formats = media.stream().map(MusicBrainzRelease.Medium::format)
                .map(this::blankToNull).filter(Objects::nonNull).toList();
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < formats.size(); ) {
            int count = 1;
            while (i + count < formats.size() && formats.get(i + count).equals(formats.get(i))) {
                count++;
            }
            parts.add((count > 1 ? count + "×" : "") + formats.get(i));
            i += count;
        }
        return parts.isEmpty() ? null : String.join(" + ", parts);
    }

    @Named("firstLabelName")
    String firstLabelName(List<MusicBrainzRelease.LabelInfo> labelInfo) {
        return labelInfo == null ? null : labelInfo.stream()
                .map(MusicBrainzRelease.LabelInfo::label).filter(Objects::nonNull)
                .map(label -> blankToNull(label.name())).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Named("firstCatalogNumber")
    String firstCatalogNumber(List<MusicBrainzRelease.LabelInfo> labelInfo) {
        // "[none]" betekent bij MusicBrainz: geen catalogusnummer.
        return labelInfo == null ? null : labelInfo.stream()
                .map(info -> blankToNull(info.catalogNumber()))
                .filter(catno -> catno != null && !"[none]".equalsIgnoreCase(catno))
                .findFirst().orElse(null);
    }

    @Named("frontThumbnailUrl")
    String frontThumbnailUrl(String mbid, @Context String coverArtBaseUrl) {
        return mbid == null || coverArtBaseUrl == null ? null : coverArtBaseUrl + "/release/" + mbid + "/front-250";
    }

    /**
     * Alle media achter elkaar; {@code discNumber} is de positie van het medium, {@code sortOrder} loopt
     * door. De trackartiest wordt alleen gezet als die afwijkt van de releaseartiest.
     */
    @Named("tracks")
    List<TrackDto> tracks(MusicBrainzRelease release) {
        List<TrackDto> tracks = new ArrayList<>();
        if (release == null || release.media() == null) {
            return tracks;
        }
        String releaseArtist = artistCredit(release.artistCredit());
        for (int m = 0; m < release.media().size(); m++) {
            MusicBrainzRelease.Medium medium = release.media().get(m);
            int discNumber = medium.position() == null ? m + 1 : medium.position();
            if (medium.tracks() == null) {
                continue;
            }
            for (MusicBrainzRelease.Track track : medium.tracks()) {
                String title = blankToNull(track.title());
                if (title == null) {
                    continue;
                }
                String artist = artistCredit(track.artistCredit());
                tracks.add(new TrackDto(discNumber, blankToNull(track.number()), tracks.size() + 1, title,
                        durationSeconds(track.length()),
                        Objects.equals(artist, releaseArtist) ? null : artist));
            }
        }
        return tracks;
    }

    @Named("blankToNull")
    String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** Milliseconden → hele seconden (afgerond). */
    static Integer durationSeconds(Long lengthMillis) {
        return lengthMillis == null || lengthMillis <= 0 ? null : Math.toIntExact(Math.round(lengthMillis / 1000.0));
    }

    static MediaType mediaTypeOf(String format) {
        if (format == null) {
            return MediaType.OTHER;
        }
        String f = format.toLowerCase(Locale.ROOT);
        // Volgorde telt: "Blu-spec CD" is een cd, "HD-DVD" een dvd.
        if (f.contains("blu-ray")) {
            return MediaType.BLU_RAY;
        }
        if (f.contains("dvd")) {
            return MediaType.DVD;
        }
        if (f.contains("vinyl")) {
            return MediaType.VINYL;
        }
        if (f.contains("cassette")) {
            return MediaType.CASSETTE;
        }
        if (f.contains("cd")) {
            return MediaType.CD;
        }
        return MediaType.OTHER;
    }
}
