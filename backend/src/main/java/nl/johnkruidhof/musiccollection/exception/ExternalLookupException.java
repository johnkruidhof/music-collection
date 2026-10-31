package nl.johnkruidhof.musiccollection.exception;

/** Een externe metadatabron (Discogs, MusicBrainz) gaf een fout of was onbereikbaar. */
public class ExternalLookupException extends RuntimeException {

    private final String source;

    public ExternalLookupException(String source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
