package nl.johnkruidhof.musiccollection.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.client.AudioRecognizer;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemSummaryDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognitionResultDto;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognizedTrackDto;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.RecognitionUnavailableException;
import nl.johnkruidhof.musiccollection.mapper.CollectionItemMapper;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;

/**
 * Luisteren (F-30): fragment herkennen, dan koppelen aan de eigen collectie (artiest + tracktitel of album)
 * en aan releases om toe te voegen (zoeken op artiest + album via de bestaande lookup).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecognitionService {

    static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final String NO_MATCH = "#";

    private final AudioRecognizer audioRecognizer;
    private final LookupService lookupService;
    private final UserService userService;
    private final CollectionItemRepository collectionItemRepository;
    private final CollectionItemMapper collectionItemMapper;
    private final TransactionTemplate transactionTemplate;

    public boolean enabled() {
        return audioRecognizer.enabled();
    }

    /** Herkennen gebeurt buiten een transactie (externe aanroep van enkele seconden). */
    public RecognitionResultDto recognize(Jwt jwt, byte[] audio, String fileName) {
        if (!audioRecognizer.enabled()) {
            throw new RecognitionUnavailableException();
        }
        if (audio == null || audio.length == 0) {
            throw new InvalidRequestException("Er is geen opname meegestuurd.");
        }
        if (audio.length > MAX_BYTES) {
            throw new InvalidRequestException("De opname is te groot (max. 10 MB).");
        }
        Optional<RecognizedTrackDto> recognized = audioRecognizer.recognize(audio, safeFileName(fileName));
        if (recognized.isEmpty() || isBlank(recognized.get().artist()) || isBlank(recognized.get().title())) {
            return RecognitionResultDto.NOTHING;
        }
        RecognizedTrackDto track = recognized.get();
        log.debug("Herkend: {} - {} ({})", track.artist(), track.title(), track.album());
        return new RecognitionResultDto(track, inCollection(jwt, track), candidates(jwt, track));
    }

    private List<CollectionItemSummaryDto> inCollection(Jwt jwt, RecognizedTrackDto track) {
        return transactionTemplate.execute(status -> {
            UUID userId = userService.getOrCreate(jwt).getId();
            return collectionItemRepository.findByRecognizedTrack(userId, contains(mainArtist(track.artist())),
                            contains(baseTitle(track.title())),
                            isBlank(track.album()) ? NO_MATCH : contains(baseTitle(track.album())))
                    .stream().map(collectionItemMapper::toSummary).toList();
        });
    }

    /** Releases met dit album (of als single de titel); een storende bron maakt de herkenning niet stuk. */
    private List<ReleaseCandidateDto> candidates(Jwt jwt, RecognizedTrackDto track) {
        String release = isBlank(track.album()) ? track.title() : baseTitle(track.album());
        try {
            return lookupService.search(jwt, new ReleaseSearchCriteria(mainArtist(track.artist()), release, null, null));
        } catch (ExternalLookupException ex) {
            log.warn("Zoeken naar releases voor herkend nummer mislukt: {}", ex.getMessage());
            return List.of();
        }
    }

    /** "Radiohead feat. Thom Yorke" → "Radiohead"; "Simon & Garfunkel" blijft heel. */
    static String mainArtist(String artist) {
        return artist.split("(?i)\\s+(feat\\.?|ft\\.?|featuring|with|x)\\s+")[0].strip();
    }

    /** "Karma Police (Remastered 2017)" → "Karma Police"; "Airbag - Live" → "Airbag". */
    static String baseTitle(String title) {
        String withoutBrackets = title.replaceAll("\\s*[(\\[].*?[)\\]]", "");
        String withoutSuffix = withoutBrackets.replaceAll("(?i)\\s+-\\s+(live|remaster(ed)?|single|radio edit|mono|stereo).*$", "");
        return withoutSuffix.isBlank() ? title.strip() : withoutSuffix.strip();
    }

    private static String contains(String value) {
        return "%" + value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
                + "%";
    }

    private static String safeFileName(String fileName) {
        return fileName == null || !fileName.matches("[\\w.-]{1,100}") ? "opname.webm" : fileName;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
