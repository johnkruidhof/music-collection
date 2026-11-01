package nl.johnkruidhof.musiccollection.dto.genre;

/**
 * Resultaat van het importeren van een boomstructuur (F-28).
 *
 * @param artistsLinked artiesten waarvan het genre nu bekend is (nieuw of gewijzigd)
 * @param itemsAssigned collectie-items zonder genre die er nu een hebben
 * @param wishesAssigned wensen zonder genre die er nu een hebben
 */
public record GenreImportResultDto(
        int genresCreated,
        int subgenresCreated,
        int artistsLinked,
        int itemsAssigned,
        int wishesAssigned) {
}
