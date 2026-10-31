package nl.johnkruidhof.musiccollection.client.audd;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Antwoord van {@code POST https://api.audd.io/}: {@code status} "success" (met {@code result}, of
 * {@code null} als er niets herkend is) of "error" (met {@code error}).
 */
public record AuddResponse(String status, AuddResult result, Error error) {

    public record Error(@JsonProperty("error_code") int code, @JsonProperty("error_message") String message) {
    }
}
