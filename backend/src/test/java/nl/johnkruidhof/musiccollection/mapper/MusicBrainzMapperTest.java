package nl.johnkruidhof.musiccollection.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nl.johnkruidhof.musiccollection.client.musicbrainz.MusicBrainzRelease;
import nl.johnkruidhof.musiccollection.entity.MediaType;

class MusicBrainzMapperTest {

    private final MusicBrainzMapper mapper = new MusicBrainzMapperImpl();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "CD|CD", "Enhanced CD|CD", "Blu-spec CD|CD", "Hybrid SACD|CD",
            "12\" Vinyl|VINYL", "Vinyl|VINYL", "7\" Vinyl|VINYL",
            "DVD-Video|DVD", "DVD-Audio|DVD", "HD-DVD|DVD", "Blu-ray|BLU_RAY",
            "Cassette|CASSETTE", "Digital Media|OTHER", "DualDisc|OTHER"})
    void mediaTypeFromFormat(String format, MediaType expected) {
        assertThat(MusicBrainzMapper.mediaTypeOf(format)).isEqualTo(expected);
    }

    @Test
    void mediaTypeUsesFirstRecognisedMedium() {
        assertThat(mapper.mediaType(List.of(medium("Digital Media"), medium("DVD-Video"), medium("CD"))))
                .isEqualTo(MediaType.DVD);
        assertThat(mapper.mediaType(null)).isEqualTo(MediaType.OTHER);
    }

    @Test
    void formatDetailsGroupsConsecutiveEqualMedia() {
        assertThat(mapper.formatDetails(List.of(medium("CD"), medium("CD"), medium("DVD-Video"))))
                .isEqualTo("2×CD + DVD-Video");
        assertThat(mapper.formatDetails(List.of(medium("CD"), medium("DVD"), medium("CD"))))
                .isEqualTo("CD + DVD + CD");
        assertThat(mapper.formatDetails(List.of(medium(null)))).isNull();
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {"1997-06-16, 1997", "1997-06, 1997", "1997, 1997", "'', null", "null, null"})
    void yearFromDate(String date, Integer expected) {
        assertThat(mapper.yearFromDate(date)).isEqualTo(expected);
    }

    @Test
    void durationIsRoundedToSeconds() {
        assertThat(MusicBrainzMapper.durationSeconds(284_400L)).isEqualTo(284);
        assertThat(MusicBrainzMapper.durationSeconds(383_600L)).isEqualTo(384);
        assertThat(MusicBrainzMapper.durationSeconds(null)).isNull();
        assertThat(MusicBrainzMapper.durationSeconds(0L)).isNull();
    }

    @Test
    void artistCreditUsesJoinPhrases() {
        assertThat(mapper.artistCredit(List.of(
                new MusicBrainzRelease.ArtistCredit("Simon", " & "),
                new MusicBrainzRelease.ArtistCredit("Garfunkel", "")))).isEqualTo("Simon & Garfunkel");
        assertThat(mapper.artistCredit(List.of())).isNull();
    }

    @Test
    void placeholderCatalogNumberIsIgnored() {
        assertThat(mapper.firstCatalogNumber(List.of(
                new MusicBrainzRelease.LabelInfo("[none]", new MusicBrainzRelease.Label("Parlophone")),
                new MusicBrainzRelease.LabelInfo("7243 8 55229 1 3", null)))).isEqualTo("7243 8 55229 1 3");
    }

    private static MusicBrainzRelease.Medium medium(String format) {
        return new MusicBrainzRelease.Medium(null, format, null);
    }
}
