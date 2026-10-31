package nl.johnkruidhof.musiccollection.exception;

/** Luisteren is niet ingesteld (geen API-sleutel voor de herkenningsdienst). */
public class RecognitionUnavailableException extends RuntimeException {

    public RecognitionUnavailableException() {
        super("Luisteren is niet ingesteld: zet AUDD_API_TOKEN in .env en herstart de backend.");
    }
}
