package nl.johnkruidhof.musiccollection.exception;

/** De geüploade cover is geen leesbare afbeelding (of een niet-ondersteund formaat). */
public class UnsupportedImageException extends InvalidRequestException {

    public UnsupportedImageException(String message) {
        super(message);
    }
}
