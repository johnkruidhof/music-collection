package nl.johnkruidhof.musiccollection.exception;

/** Resource bestaat niet, of is niet van de ingelogde gebruiker (bewust niet te onderscheiden). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s met id %s niet gevonden".formatted(resource, id));
    }
}
