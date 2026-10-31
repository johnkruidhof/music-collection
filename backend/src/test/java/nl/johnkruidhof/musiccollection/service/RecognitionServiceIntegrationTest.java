package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import nl.johnkruidhof.musiccollection.TestDatabase;
import nl.johnkruidhof.musiccollection.TestcontainersConfiguration;
import nl.johnkruidhof.musiccollection.client.AudioRecognizer;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemSummaryDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognitionResultDto;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognizedTrackDto;
import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;

/** Luisteren (F-30): koppeling van een herkend nummer aan de eigen collectie en aan kandidaten. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RecognitionServiceIntegrationTest {

    private static final Jwt ALICE = jwt("alice-sub", "alice");
    private static final Jwt BOB = jwt("bob-sub", "bob");

    @Autowired
    private RecognitionService recognitionService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private AudioRecognizer audioRecognizer;

    @MockitoBean
    private ExternalLookupService externalLookupService;

    @BeforeEach
    void setUp() {
        TestDatabase.clean(jdbc);
        given(audioRecognizer.enabled()).willReturn(true);
        given(audioRecognizer.recognize(any(), any())).willReturn(Optional.of(new RecognizedTrackDto(
                "Radiohead feat. Someone", "Karma Police (Remastered)", "OK Computer OKNOTOK", 2017, null, null)));
    }

    @Test
    void recognizedTrackIsMatchedToOwnReleasesByTrackTitleAndToCandidates() {
        add(ALICE, "Radiohead", "OK Computer", List.of("Airbag", "Karma Police"));
        add(ALICE, "Radiohead", "Kid A", List.of("Idioteque"));
        add(BOB, "Radiohead", "OK Computer", List.of("Karma Police"));
        given(externalLookupService.search(any())).willReturn(List.of(new ReleaseCandidateDto(MetadataSource.DISCOGS,
                "1", "Radiohead", "OK Computer OKNOTOK 1997 2017", 2017, MediaType.VINYL, null, null, null, null, null,
                null, null, null)));

        RecognitionResultDto result = recognitionService.recognize(ALICE, new byte[] {1, 2}, "opname.webm");

        assertThat(result.track().title()).isEqualTo("Karma Police (Remastered)");
        assertThat(result.inCollection()).extracting(CollectionItemSummaryDto::title).containsExactly("OK Computer");
        assertThat(result.candidates()).singleElement().satisfies(c -> assertThat(c.ownedCount()).isZero());
    }

    @Test
    void failingReleaseSearchDoesNotBreakRecognition() {
        add(ALICE, "Radiohead", "OK Computer", List.of("Karma Police"));
        given(externalLookupService.search(any())).willThrow(new ExternalLookupException("Discogs", "plat", null));

        RecognitionResultDto result = recognitionService.recognize(ALICE, new byte[] {1}, "opname.webm");

        assertThat(result.inCollection()).hasSize(1);
        assertThat(result.candidates()).isEmpty();
    }

    private void add(Jwt jwt, String artist, String title, List<String> tracks) {
        List<TrackDto> trackDtos = tracks.stream().map(t -> new TrackDto(1, null, 0, t, null, null)).toList();
        collectionService.add(jwt, new AddToCollectionRequest(null, null,
                new ManualReleaseRequest(null, artist, title, 1997, MediaType.CD, null, null, null, null, trackDtos),
                null));
    }

    private static Jwt jwt(String subject, String username) {
        return Jwt.withTokenValue("token-" + UUID.randomUUID()).header("alg", "none")
                .subject(subject).claim("preferred_username", username).build();
    }
}
