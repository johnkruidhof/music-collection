package nl.johnkruidhof.musiccollection.client.audd;

import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.client.AudioRecognizer;
import nl.johnkruidhof.musiccollection.config.AppProperties;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognizedTrackDto;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.mapper.RecognitionMapper;

/**
 * AudD (https://docs.audd.io): multipart-upload naar {@code POST /} met {@code api_token} en {@code file}.
 * Foutcodes: 901 = limiet bereikt / geen token, 900 = ongeldig token, 300/500 = opname onbruikbaar.
 */
@Slf4j
@Component
public class AuddRecognizer implements AudioRecognizer {

    static final String SOURCE_NAME = "AudD";

    private final RestClient restClient;
    private final AppProperties.Audd settings;
    private final RecognitionMapper recognitionMapper;

    public AuddRecognizer(RestClient auddRestClient, AppProperties appProperties, RecognitionMapper recognitionMapper) {
        this.restClient = auddRestClient;
        this.settings = appProperties.audd();
        this.recognitionMapper = recognitionMapper;
    }

    @Override
    public boolean enabled() {
        return settings.enabled();
    }

    @Override
    public Optional<RecognizedTrackDto> recognize(byte[] audio, String fileName) {
        // Geen MultipartBodyBuilder: die vereist Reactive Streams op het classpath.
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("api_token", settings.apiToken());
        body.add("file", new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        AuddResponse response;
        try {
            response = restClient.post()
                    .uri("/")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(AuddResponse.class);
        } catch (RestClientException ex) {
            throw new ExternalLookupException(SOURCE_NAME, "AudD niet bereikbaar: " + ex.getMessage(), ex);
        }
        if (response == null) {
            throw new ExternalLookupException(SOURCE_NAME, "Leeg antwoord van AudD", null);
        }
        if ("error".equals(response.status()) && response.error() != null) {
            throw toException(response.error());
        }
        return Optional.ofNullable(response.result()).map(recognitionMapper::toDto);
    }

    private static RuntimeException toException(AuddResponse.Error error) {
        log.warn("AudD-fout {}: {}", error.code(), error.message());
        return switch (error.code()) {
            case 300, 500 -> new InvalidRequestException(
                    "De opname is te kort of onduidelijk om te herkennen. Probeer het opnieuw, dichter bij de speaker.");
            case 400 -> new InvalidRequestException("De opname is te groot (max. 10 MB).");
            case 901 -> new ExternalLookupException(SOURCE_NAME,
                    "Het tegoed van AudD is op (of er is geen API-sleutel).", null);
            default -> new ExternalLookupException(SOURCE_NAME, "AudD-fout " + error.code() + ": " + error.message(),
                    null);
        };
    }
}
