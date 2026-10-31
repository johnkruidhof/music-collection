package nl.johnkruidhof.musiccollection.exception;

/** Functioneel ongeldig verzoek dat niet met Bean Validation af te vangen is. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
