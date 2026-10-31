package nl.johnkruidhof.musiccollection.client;

import java.util.Optional;

import nl.johnkruidhof.musiccollection.dto.recognition.RecognizedTrackDto;

/**
 * Herkent muziek in een kort (microfoon)fragment (F-30). Implementatie: AudD; een andere dienst (bv.
 * ACRCloud) kan hier later achter.
 */
public interface AudioRecognizer {

    /** Of de dienst is ingesteld (API-sleutel aanwezig). */
    boolean enabled();

    /**
     * @param audio       opname, bv. 10 s WebM/Opus uit de browser (max. 10 MB)
     * @param fileName    bestandsnaam met extensie, helpt de dienst het formaat te herkennen
     * @return het herkende nummer, of leeg als er niets herkend is
     */
    Optional<RecognizedTrackDto> recognize(byte[] audio, String fileName);
}
