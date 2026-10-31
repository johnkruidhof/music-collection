package nl.johnkruidhof.musiccollection.service;

import java.util.List;

import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;

/**
 * Barcode-normalisatie. Een UPC-A (12 cijfers) en de EAN-13 met voorloopnul zijn dezelfde code;
 * scanners en databases gebruiken beide vormen door elkaar.
 */
public final class Barcodes {

    private Barcodes() {
    }

    /** Spaties en streepjes weg; 8–14 cijfers (EAN-8, UPC-A, EAN-13, GTIN-14), anders 400. */
    public static String normalize(String barcode) {
        String digits = barcode == null ? "" : barcode.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{8,14}")) {
            throw new InvalidRequestException("Ongeldige barcode: verwacht 8 tot 14 cijfers.");
        }
        return digits;
    }

    /** De genormaliseerde code plus de UPC-A/EAN-13-tegenhanger, in die volgorde. */
    public static List<String> variants(String normalized) {
        if (normalized.length() == 12) {
            return List.of(normalized, "0" + normalized);
        }
        if (normalized.length() == 13 && normalized.startsWith("0")) {
            return List.of(normalized, normalized.substring(1));
        }
        return List.of(normalized);
    }
}
