package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.support.TransactionTemplate;

import nl.johnkruidhof.musiccollection.client.AudioRecognizer;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognitionResultDto;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.RecognitionUnavailableException;
import nl.johnkruidhof.musiccollection.mapper.CollectionItemMapper;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;

class RecognitionServiceTest {

    private final AudioRecognizer recognizer = mock(AudioRecognizer.class);
    private final LookupService lookupService = mock(LookupService.class);
    private final RecognitionService service = new RecognitionService(recognizer, lookupService,
            mock(UserService.class), mock(CollectionItemRepository.class), mock(CollectionItemMapper.class),
            mock(TransactionTemplate.class));
    private final Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("alice").build();

    @Test
    void notConfiguredIsReported() {
        given(recognizer.enabled()).willReturn(false);

        assertThatThrownBy(() -> service.recognize(jwt, new byte[] {1}, "opname.webm", false))
                .isInstanceOf(RecognitionUnavailableException.class);
    }

    @Test
    void emptyRecordingIsRejected() {
        given(recognizer.enabled()).willReturn(true);

        assertThatThrownBy(() -> service.recognize(jwt, new byte[0], "opname.webm", false))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void nothingRecognizedSkipsCollectionAndSearch() {
        given(recognizer.enabled()).willReturn(true);
        given(recognizer.recognize(any(), any())).willReturn(Optional.empty());

        assertThat(service.recognize(jwt, new byte[] {1}, "../../etc/passwd", false)).isEqualTo(RecognitionResultDto.NOTHING);
        verifyNoInteractions(lookupService);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Radiohead|Radiohead", "Radiohead feat. Thom Yorke|Radiohead", "Daft Punk ft. Pharrell|Daft Punk",
            "Simon & Garfunkel|Simon & Garfunkel", "Calvin Harris featuring Rihanna|Calvin Harris"})
    void mainArtist(String artist, String expected) {
        assertThat(RecognitionService.mainArtist(artist)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Karma Police|Karma Police", "Karma Police (Remastered 2017)|Karma Police", "Airbag - Live|Airbag",
            "OK Computer [Collector's Edition]|OK Computer", "(What's the Story) Morning Glory?|Morning Glory?"})
    void baseTitle(String title, String expected) {
        assertThat(RecognitionService.baseTitle(title)).isEqualTo(expected);
    }
}
