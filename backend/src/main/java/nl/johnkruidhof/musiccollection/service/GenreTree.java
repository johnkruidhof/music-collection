package nl.johnkruidhof.musiccollection.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leest genres uit tekst (F-28):
 * <ul>
 * <li>{@link #parseTree}: een boomstructuur van een muziekmap ({@code |-- }-notatie). Niveau 1 = genre;
 * op niveau 2 staat een artiest, of een <b>subgenre</b> als de items eronder geen jaartal hebben (artiesten)
 * en die daaronder wel (titels, bv. "1997 - OK Computer"). Zo werkt het voor "Hardrock - Metal" zonder dat
 * die naam hard in de code staat.</li>
 * <li>{@link #parseList}: een eenvoudige lijst, subgenres springen in (de standaardgenres).</li>
 * </ul>
 */
final class GenreTree {

    private static final Pattern TREE_LINE = Pattern.compile("^((?: +\\|)+)-- (.+)$");
    private static final Pattern STARTS_WITH_YEAR = Pattern.compile("^\\(?\\d{4}\\b");

    record ParsedGenre(String name, List<ParsedSubgenre> subgenres, List<String> artists) {
    }

    record ParsedSubgenre(String name, List<String> artists) {
    }

    private static final class Node {
        final String name;
        final List<Node> children = new ArrayList<>();

        Node(String name) {
            this.name = name;
        }
    }

    private GenreTree() {
    }

    static List<ParsedGenre> parseTree(String text) {
        Node root = new Node(".");
        List<Node> stack = new ArrayList<>(List.of(root));
        for (String line : text.split("\\R")) {
            Matcher m = TREE_LINE.matcher(line);
            if (!m.matches() || m.group(2).isBlank()) {
                continue;
            }
            int depth = (int) m.group(1).chars().filter(c -> c == '|').count();
            while (stack.size() > depth) {
                stack.removeLast();
            }
            if (stack.size() < depth) {
                continue; // inspringing klopt niet (sprong van meer dan één niveau): regel overslaan
            }
            Node node = new Node(m.group(2).strip());
            stack.getLast().children.add(node);
            stack.add(node);
        }
        List<ParsedGenre> genres = new ArrayList<>();
        for (Node genre : root.children) {
            List<ParsedSubgenre> subgenres = new ArrayList<>();
            List<String> artists = new ArrayList<>();
            for (Node child : genre.children) {
                if (isSubgenre(child)) {
                    subgenres.add(new ParsedSubgenre(child.name, child.children.stream().map(n -> n.name).toList()));
                } else {
                    artists.add(child.name);
                }
            }
            genres.add(new ParsedGenre(genre.name, subgenres, artists));
        }
        return genres;
    }

    static List<ParsedGenre> parseList(String text) {
        List<ParsedGenre> genres = new ArrayList<>();
        for (String line : text.split("\\R")) {
            if (line.isBlank() || line.strip().startsWith("#")) {
                continue;
            }
            if (Character.isWhitespace(line.charAt(0)) && !genres.isEmpty()) {
                genres.getLast().subgenres().add(new ParsedSubgenre(line.strip(), List.of()));
            } else {
                genres.add(new ParsedGenre(line.strip(), new ArrayList<>(), List.of()));
            }
        }
        return genres;
    }

    /** Subgenre: de kinderen (artiesten) hebben geen jaartal, de kleinkinderen (titels) meestal wel. */
    private static boolean isSubgenre(Node node) {
        if (node.children.isEmpty() || node.children.stream().anyMatch(GenreTree::startsWithYear)) {
            return false;
        }
        List<Node> grandchildren = node.children.stream().flatMap(c -> c.children.stream()).toList();
        long withYear = grandchildren.stream().filter(GenreTree::startsWithYear).count();
        return !grandchildren.isEmpty() && withYear * 2 >= grandchildren.size();
    }

    private static boolean startsWithYear(Node node) {
        return STARTS_WITH_YEAR.matcher(node.name).find();
    }
}
