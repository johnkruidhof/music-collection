package nl.johnkruidhof.musiccollection.client.audd;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import nl.johnkruidhof.musiccollection.config.AppProperties;
import nl.johnkruidhof.musiccollection.config.AuddClientConfig;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognizedTrackDto;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.mapper.RecognitionMapperImpl;

class AuddRecognizerTest {

    @RegisterExtension
    static WireMockExtension audd = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private AuddRecognizer recognizer;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(null, null,
                new AppProperties.MusicBrainz("http://unused", "http://unused", "MusicCollectionTest/1.0", null), null,
                new AppProperties.Audd(audd.baseUrl(), "test-token"));
        recognizer = new AuddRecognizer(AuddClientConfig.createAuddRestClient(RestClient.builder(), properties),
                properties, new RecognitionMapperImpl());
    }

    @Test
    void uploadsRecordingWithTokenAndMapsResult() {
        audd.stubFor(post("/").willReturn(okJson("""
                {"status":"success","result":{"artist":"Radiohead","title":"Karma Police","album":"OK Computer",
                 "release_date":"1997-08-25","label":"Parlophone","timecode":"01:12","song_link":"https://lis.tn/KarmaPolice"}}""")));

        RecognizedTrackDto track = recognizer.recognize(new byte[] {1, 2, 3}, "opname.webm").orElseThrow();

        assertThat(track).isEqualTo(new RecognizedTrackDto("Radiohead", "Karma Police", "OK Computer", 1997,
                "Parlophone", "https://lis.tn/KarmaPolice"));
        audd.verify(postRequestedFor(urlEqualTo("/"))
                .withAnyRequestBodyPart(aMultipart().withName("api_token").withBody(equalTo("test-token")))
                .withAnyRequestBodyPart(aMultipart().withName("file")));
    }

    @Test
    void nothingRecognizedIsEmpty() {
        audd.stubFor(post("/").willReturn(okJson("{\"status\":\"success\",\"result\":null}")));

        assertThat(recognizer.recognize(new byte[] {1}, "opname.webm")).isEmpty();
    }

    @Test
    void unusableRecordingIsAClientError() {
        audd.stubFor(post("/").willReturn(okJson("""
                {"status":"error","error":{"error_code":300,"error_message":"Recognition failed: a problem with fingerprints creating."}}""")));

        assertThatThrownBy(() -> recognizer.recognize(new byte[] {1}, "opname.webm"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("te kort of onduidelijk");
    }

    @Test
    void exhaustedLimitAndServerErrorsAreExternalFailures() {
        audd.stubFor(post("/").willReturn(okJson("""
                {"status":"error","error":{"error_code":901,"error_message":"Recognition failed: authorization failed: no api_token passed and the limit was reached."}}""")));
        assertThatThrownBy(() -> recognizer.recognize(new byte[] {1}, "opname.webm"))
                .isInstanceOf(ExternalLookupException.class)
                .hasMessageContaining("tegoed");

        audd.stubFor(post("/").willReturn(aResponse().withStatus(502)));
        assertThatThrownBy(() -> recognizer.recognize(new byte[] {1}, "opname.webm"))
                .isInstanceOf(ExternalLookupException.class)
                .extracting("source").isEqualTo("AudD");
    }

    @Test
    void enabledOnlyWithToken() {
        assertThat(recognizer.enabled()).isTrue();
        assertThat(new AppProperties.Audd(null, " ").enabled()).isFalse();
    }
}
