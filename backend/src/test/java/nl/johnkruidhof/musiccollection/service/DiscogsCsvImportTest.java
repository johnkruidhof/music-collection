package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;

class DiscogsCsvImportTest {

    static final String EXPORT = """
            Catalog#,Artist,Title,Label,Format,Rating,Released,release_id,CollectionFolder,Date Added,Collection Media Condition,Collection Sleeve Condition,Collection Notes
            NODATA 02,Radiohead,OK Computer,Parlophone,"2xLP, Album",,1997,2149389,Uncategorized,2023-04-12 10:11:12,Near Mint (NM or M-),Very Good Plus (VG+),"Gekocht in Utrecht, mooi"
            ,De Dijk,Nooit Meer Slapen,Polydor,LP,,1985,abc,Uncategorized,2023-04-12 10:11:12,,,
            """;

    @Test
    void readsDiscogsCollectionExport() {
        var rows = DiscogsCsvImport.parse(EXPORT);

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()).isEqualTo(new DiscogsCsvImport.Row(2, "2149389", "Radiohead", "OK Computer",
                MediaCondition.NEAR_MINT, MediaCondition.VERY_GOOD_PLUS, "Gekocht in Utrecht, mooi"));
        assertThat(rows.get(1).releaseId()).isEqualTo("abc");
        assertThat(rows.get(1).conditionMedia()).isNull();
    }

    @Test
    void fileWithoutReleaseIdColumnIsRejected() {
        assertThatThrownBy(() -> DiscogsCsvImport.parse("Artiest;Titel\nRadiohead;OK Computer"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("release_id");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", delimiter = '|', value = {
            "Mint (M)|MINT", "Near Mint (NM or M-)|NEAR_MINT", "Very Good Plus (VG+)|VERY_GOOD_PLUS",
            "Very Good (VG)|VERY_GOOD", "Good Plus (G+)|GOOD", "Good (G)|GOOD", "Fair (F)|POOR", "Poor (P)|POOR",
            "Generic|null", "No Cover|null", "Not Graded|null", "''|null"})
    void conditionsAreMappedToGoldmine(String discogs, MediaCondition expected) {
        assertThat(DiscogsCsvImport.condition(discogs)).isEqualTo(expected);
    }
}
