package nl.johnkruidhof.musiccollection.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;

/** Minimale CSV-lezer/schrijver (RFC 4180: aanhalingstekens, "" als escape, regeleinden binnen velden). */
final class Csv {

    private Csv() {
    }

    /** Alle rijen; een BOM aan het begin wordt genegeerd, lege regels worden overgeslagen. */
    static List<List<String>> parse(String text, char separator) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int start = !text.isEmpty() && text.charAt(0) == '﻿' ? 1 : 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == separator) {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                addRow(rows, row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (quoted) {
            throw new InvalidRequestException("Ongeldig CSV-bestand: een aanhalingsteken wordt niet afgesloten.");
        }
        row.add(field.toString());
        addRow(rows, row);
        return rows;
    }

    private static void addRow(List<List<String>> rows, List<String> row) {
        if (!(row.size() == 1 && row.getFirst().isBlank())) {
            rows.add(row);
        }
    }

    static <T> String write(List<String> header, List<T> items, Function<T, List<String>> columns, char separator) {
        StringBuilder out = new StringBuilder();
        appendRow(out, header, separator);
        for (T item : items) {
            appendRow(out, columns.apply(item), separator);
        }
        return out.toString();
    }

    private static void appendRow(StringBuilder out, List<String> values, char separator) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(separator);
            }
            out.append(escape(values.get(i), separator));
        }
        out.append("\r\n");
    }

    static String escape(String value, char separator) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(separator) >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        return needsQuotes ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }
}
