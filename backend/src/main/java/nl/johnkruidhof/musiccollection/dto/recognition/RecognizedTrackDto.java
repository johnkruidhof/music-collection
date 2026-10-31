package nl.johnkruidhof.musiccollection.dto.recognition;

/** Het herkende nummer (F-30). {@code songLink} verwijst naar een pagina met streamingdiensten. */
public record RecognizedTrackDto(
        String artist,
        String title,
        String album,
        Integer releaseYear,
        String label,
        String songLink) {
}
