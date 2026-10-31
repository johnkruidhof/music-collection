package nl.johnkruidhof.musiccollection.client.musicbrainz;

/** Gevonden cover-URL's voor een release; beide {@code null} als er geen voorkant is. */
public record CoverArt(String imageUrl, String thumbnailUrl) {

    public static final CoverArt NONE = new CoverArt(null, null);
}
