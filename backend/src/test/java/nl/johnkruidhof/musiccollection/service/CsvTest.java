package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;

class CsvTest {

    @Test
    void parsesQuotesEscapedQuotesNewlinesAndBom() {
        String csv = "﻿a,b,c\r\n1,\"Simon, Garfunkel\",\"zei \"\"hoi\"\"\nen ging\"\n\n2,,x\n";

        assertThat(Csv.parse(csv, ',')).containsExactly(
                List.of("a", "b", "c"),
                List.of("1", "Simon, Garfunkel", "zei \"hoi\"\nen ging"),
                List.of("2", "", "x"));
    }

    @Test
    void unclosedQuoteIsRejected() {
        assertThatThrownBy(() -> Csv.parse("a,\"b\n", ',')).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void writtenCsvCanBeReadBack() {
        List<List<String>> rows = List.of(List.of("Radiohead", "OK; Computer", "zei \"hoi\""), List.of("", "x", "a\nb"));

        String csv = Csv.write(List.of("A", "B", "C"), rows, row -> row, ';');

        assertThat(Csv.parse(csv, ';')).containsExactly(List.of("A", "B", "C"), rows.get(0), rows.get(1));
    }
}
