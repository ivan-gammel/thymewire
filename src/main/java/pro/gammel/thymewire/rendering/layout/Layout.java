package pro.gammel.thymewire.rendering.layout;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public record Layout(List<String> paths) {

    public boolean matches(String path) {
        return paths.stream().anyMatch(byPattern(path));
    }

    private static Predicate<? super String> byPattern(String path) {
        return pattern -> matchesAntPattern(pattern, path);
    }

    /**
     * Matches a path against an Ant-style pattern.
     * Supports:
     * - ? matches one character
     * - * matches zero or more characters within a path segment
     * - ** matches zero or more path segments
     *
     * @param pattern the Ant-style pattern
     * @param path the path to match
     * @return true if the path matches the pattern
     */
    private static boolean matchesAntPattern(String pattern, String path) {
        if (pattern.equals(path)) {
            return true;
        }

        // Convert Ant pattern to regex
        String regex = antPatternToRegex(pattern);
        return Pattern.matches(regex, path);
    }

    /**
     * Converts an Ant-style pattern to a regular expression.
     *
     * @param pattern the Ant-style pattern
     * @return the equivalent regex pattern
     */
    private static String antPatternToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        int i = 0;
        int length = pattern.length();

        while (i < length) {
            char c = pattern.charAt(i);

            if (c == '*') {
                if (i + 1 < length && pattern.charAt(i + 1) == '*') {
                    // ** - matches zero or more path segments
                    if (i + 2 < length && pattern.charAt(i + 2) == '/') {
                        // **/ pattern
                        regex.append("(?:.*?/)?");
                        i += 3;
                    } else if (i + 2 == length) {
                        // ** at the end
                        regex.append(".*");
                        i += 2;
                    } else {
                        // ** in the middle without /
                        regex.append(".*");
                        i += 2;
                    }
                } else {
                    // * - matches zero or more characters within a segment
                    regex.append("[^/]*");
                    i++;
                }
            } else if (c == '?') {
                // ? - matches exactly one character
                regex.append("[^/]");
                i++;
            } else if (c == '.' || c == '(' || c == ')' || c == '+' || c == '|' ||
                       c == '^' || c == '$' || c == '@' || c == '%' || c == '[' || c == ']' ||
                       c == '{' || c == '}' || c == '\\') {
                // Escape regex special characters
                regex.append('\\').append(c);
                i++;
            } else {
                regex.append(c);
                i++;
            }
        }

        return regex.toString();
    }

}
