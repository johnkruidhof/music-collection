package nl.johnkruidhof.musiccollection.exception;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Vertaalt exceptions naar RFC 9457 Problem Details ({@code application/problem+json}).
 * Standaard Spring MVC-fouten (404, 405, validatie, ...) worden door de superklasse afgehandeld.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_BASE = "https://musiccollection.local/problems/";

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Niet gevonden", ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-request", "Ongeldig verzoek", ex.getMessage());
    }

    @ExceptionHandler(ExternalLookupException.class)
    ProblemDetail handleExternalLookup(ExternalLookupException ex) {
        log.warn("Externe lookup bij {} mislukt: {}", ex.getSource(), ex.getMessage(), ex);
        ProblemDetail pd = problem(HttpStatus.BAD_GATEWAY, "external-lookup-failed",
                "Externe bron niet beschikbaar", "Het ophalen van gegevens bij " + ex.getSource() + " is mislukt.");
        pd.setProperty("source", ex.getSource());
        return pd;
    }

    @ExceptionHandler(RecognitionUnavailableException.class)
    ProblemDetail handleRecognitionUnavailable(RecognitionUnavailableException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "recognition-unavailable", "Luisteren niet beschikbaar",
                ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Onverwachte fout", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Interne fout",
                "Er is een onverwachte fout opgetreden.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validatie mislukt",
                "Een of meer velden zijn ongeldig.");
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "ongeldig" : fe.getDefaultMessage(),
                        (a, b) -> a));
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_BASE + type));
        pd.setTitle(title);
        return pd;
    }
}
