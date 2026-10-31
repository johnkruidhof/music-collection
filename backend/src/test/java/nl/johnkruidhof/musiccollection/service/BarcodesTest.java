package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;

class BarcodesTest {

    @ParameterizedTest
    @CsvSource({
            "724385522925, 724385522925",
            "'7 24385 52291 2', 724385522912",
            "0-77774-6439-2-8, 0777746439 28",
            "5099749129829, 5099749129829",
            "12345670, 12345670"})
    void normalizeRemovesSpacesAndDashes(String input, String expected) {
        assertThat(Barcodes.normalize(input)).isEqualTo(expected.replace(" ", ""));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1234567", "123456789012345", "72438552292X", "abc"})
    void invalidBarcodeIsRejected(String input) {
        assertThatThrownBy(() -> Barcodes.normalize(input)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void upcAAndEan13WithLeadingZeroAreEachOthersVariant() {
        assertThat(Barcodes.variants("724385522925")).containsExactly("724385522925", "0724385522925");
        assertThat(Barcodes.variants("0724385522925")).containsExactly("0724385522925", "724385522925");
        assertThat(Barcodes.variants("5099749129829")).containsExactly("5099749129829");
        assertThat(Barcodes.variants("12345670")).containsExactly("12345670");
    }
}
