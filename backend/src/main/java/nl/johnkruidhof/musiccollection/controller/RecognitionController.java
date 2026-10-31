package nl.johnkruidhof.musiccollection.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognitionResultDto;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.service.RecognitionService;

/** Luisteren: muziek herkennen uit een microfoonopname (F-30). */
@RestController
@RequestMapping("/api/recognize")
@RequiredArgsConstructor
public class RecognitionController {

    private final RecognitionService recognitionService;

    /** Of luisteren is ingesteld; de frontend verbergt de functie anders. */
    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("enabled", recognitionService.enabled());
    }

    /**
     * Multipart {@code file}: ~10 s audio (bv. WebM/Opus of MP4/AAC uit de browser), max. 10 MB.
     * {@code ?scope=collection}: alleen in de eigen collectie zoeken ("welke van mijn platen is dit?", F-32).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecognitionResultDto recognize(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "all") String scope) {
        if (!scope.equals("all") && !scope.equals("collection")) {
            throw new InvalidRequestException("scope moet 'all' of 'collection' zijn.");
        }
        try {
            return recognitionService.recognize(jwt, file.getBytes(), file.getOriginalFilename(),
                    scope.equals("collection"));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
