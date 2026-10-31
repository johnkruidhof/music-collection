package nl.johnkruidhof.musiccollection.client.audd;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Het herkende nummer volgens AudD; {@code releaseDate} is "JJJJ-MM-DD" (of leeg). */
public record AuddResult(
        String artist,
        String title,
        String album,
        @JsonProperty("release_date") String releaseDate,
        String label,
        String timecode,
        @JsonProperty("song_link") String songLink) {
}
