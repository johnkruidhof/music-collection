package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nl.johnkruidhof.musiccollection.TestFixtures;

class GenreTreeTest {

    @Test
    void readsGenresSubgenresAndArtistsFromTree() {
        List<GenreTree.ParsedGenre> genres = GenreTree.parseTree(TestFixtures.read("genres/boomstructuur-voorbeeld.txt"));

        assertThat(genres).extracting(GenreTree.ParsedGenre::name)
                .containsExactly("Alternatieve rock", "Electronica", "Hardrock - Metal", "Nederlands");
        // Artiesten op niveau 2, ook als hun titels niet met een jaartal beginnen.
        assertThat(genres.get(0).artists()).containsExactly("A Perfect Circle", "Book of Knots, The");
        assertThat(genres.get(0).subgenres()).isEmpty();
        assertThat(genres.get(1).artists()).containsExactly("James Blake");
        // Subgenres herkend aan de structuur, niet aan de naam "Hardrock - Metal".
        GenreTree.ParsedGenre metal = genres.get(2);
        assertThat(metal.artists()).isEmpty();
        assertThat(metal.subgenres()).extracting(GenreTree.ParsedSubgenre::name)
                .containsExactly("Doom Metal", "Progressive Metal");
        assertThat(metal.subgenres().get(1).artists()).containsExactly("Dream Theater", "Tool");
    }

    @Test
    void readsIndentedDefaultList() {
        List<GenreTree.ParsedGenre> genres = GenreTree.parseList("""
                # commentaar
                Rock
                Hardrock - Metal
                  Doom Metal
                  Thrash Metal

                Jazz
                """);

        assertThat(genres).extracting(GenreTree.ParsedGenre::name).containsExactly("Rock", "Hardrock - Metal", "Jazz");
        assertThat(genres.get(1).subgenres()).extracting(GenreTree.ParsedSubgenre::name)
                .containsExactly("Doom Metal", "Thrash Metal");
    }

    @Test
    void textWithoutTreeLinesGivesNoGenres() {
        assertThat(GenreTree.parseTree("Artiest;Titel\nRadiohead;OK Computer")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Cinematic Orchestra, The|cinematic orchestra", "The Cinematic Orchestra|cinematic orchestra",
            "Hüsker Dü|husker du", "Simon & Garfunkel|simon and garfunkel", "  A  Perfect   Circle |a perfect circle",
            "AC/DC|ac dc", "Dijk, De|dijk de"})
    void artistKeysAreNormalized(String artist, String key) {
        assertThat(Artists.key(artist)).isEqualTo(key);
    }
}
