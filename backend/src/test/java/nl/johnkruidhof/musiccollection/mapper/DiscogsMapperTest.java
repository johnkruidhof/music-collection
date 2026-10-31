package nl.johnkruidhof.musiccollection.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nl.johnkruidhof.musiccollection.client.discogs.DiscogsRelease;
import nl.johnkruidhof.musiccollection.entity.MediaType;

class DiscogsMapperTest {

    private final DiscogsMapper mapper = new DiscogsMapperImpl();

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "A1, 1", "B2, 1", "C1, 2", "D12, 2", "E1, 3", "B1a, 1", "D1a, 2",
            "1, 1", "12, 1", "1-3, 1", "2-5, 2", "CD2-1, 2", "2.04, 2", "DVD-1, 1", "null, 1"})
    void discNumberFromPosition(String position, int expected) {
        assertThat(DiscogsMapper.discNumber(position)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "4:44, 284", "0:59, 59", "12:05, 725", "1:02:03, 3723", "'', null", "abc, null", "null, null"})
    void durationInSeconds(String duration, Integer expected) {
        assertThat(DiscogsMapper.durationSeconds(duration)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "Vinyl, VINYL", "CD, CD", "CDr, CD", "SACD, CD", "DVD, DVD", "Blu-ray, BLU_RAY",
            "Cassette, CASSETTE", "File, OTHER", "Box Set, OTHER"})
    void mediaTypeFromFormatName(String format, MediaType expected) {
        assertThat(DiscogsMapper.mediaTypeOf(format)).isEqualTo(expected);
    }

    @Test
    void mediaTypeSkipsUnknownFormatsLikeBoxSet() {
        assertThat(mapper.mediaTypeFromNames(List.of("Box Set", "CD", "DVD"))).isEqualTo(MediaType.CD);
        assertThat(mapper.mediaTypeFromNames(List.of("All Media"))).isEqualTo(MediaType.OTHER);
        assertThat(mapper.mediaTypeFromNames(null)).isEqualTo(MediaType.OTHER);
    }

    @Test
    void searchTitleIsSplitIntoArtistAndTitle() {
        assertThat(mapper.artistFromSearchTitle("Prince (2) - Purple Rain - Deluxe")).isEqualTo("Prince");
        assertThat(mapper.titleFromSearchTitle("Prince (2) - Purple Rain - Deluxe")).isEqualTo("Purple Rain - Deluxe");
        assertThat(mapper.artistFromSearchTitle("Untitled")).isNull();
        assertThat(mapper.titleFromSearchTitle("Untitled")).isEqualTo("Untitled");
    }

    @Test
    void artistNamesAreJoinedAndCleaned() {
        assertThat(mapper.artistNames(List.of(
                new DiscogsRelease.Artist("Simon (3)", "", "&"),
                new DiscogsRelease.Artist("Garfunkel*", "", ","),
                new DiscogsRelease.Artist("Paul", "", "")))).isEqualTo("Simon & Garfunkel, Paul");
        assertThat(mapper.artistNames(List.of())).isNull();
    }

    @Test
    void formatDetailsCombinesMultipleFormats() {
        assertThat(mapper.formatDetails(List.of(
                new DiscogsRelease.Format("CD", "1", List.of("Album"), null),
                new DiscogsRelease.Format("DVD", "1", List.of("NTSC"), "Live at Wembley"))))
                .isEqualTo("CD, Album + DVD, NTSC, Live at Wembley");
    }

    @Test
    void barcodeKeepsOnlyDigitsAndSkipsNonBarcodes() {
        assertThat(mapper.firstBarcode(List.of("NODATA 02-A1", "0 77774-6439-2 8"))).isEqualTo("077774643928");
        assertThat(mapper.barcodeFromIdentifiers(List.of(
                new DiscogsRelease.Identifier("Matrix / Runout", "123456789"),
                new DiscogsRelease.Identifier("Barcode", "5 099749 129829")))).isEqualTo("5099749129829");
    }

    @Test
    void unknownYearAndNoneCatalogNumberBecomeNull() {
        assertThat(mapper.yearOrNull(0)).isNull();
        assertThat(mapper.parseYear("19xx")).isNull();
        assertThat(mapper.firstCatalogNumber(List.of(new DiscogsRelease.Label("Not On Label", "none")))).isNull();
    }

    @Test
    void mediaTypeIsTranslatedToDiscogsFormatParameter() {
        assertThat(mapper.toDiscogsFormat(MediaType.BLU_RAY)).isEqualTo("Blu-ray");
        assertThat(mapper.toDiscogsFormat(MediaType.OTHER)).isNull();
        assertThat(mapper.toDiscogsFormat(null)).isNull();
    }
}
