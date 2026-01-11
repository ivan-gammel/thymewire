package pro.gammel.thymewire.core;

import pro.gammel.thymewire.config.Mapping;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility for matching URI paths against URI template patterns and extracting variables.
 *
 * URI templates use {varName} syntax, e.g., "/events/{id}" matches "/events/123"
 * and extracts id=123.
 */
public class UriTemplateMatcher {

    /**
     * Result of a successful URI template match.
     *
     * @param mapping the mapping that matched
     * @param variables extracted URI variables (e.g., {"id": "123"})
     */
    public record MatchResult(Mapping mapping, Map<String, String> variables) {

        /**
         * Returns the template name from the mapping.
         */
        public String templateName() {
            return mapping.template();
        }

        /**
         * Returns the model ID if the mapping specifies a model parameter.
         */
        public Optional<String> modelId() {
            if (mapping.model() == null || mapping.model().isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(variables.get(mapping.model()));
        }
    }

    /**
     * Attempts to match a path against a list of mappings.
     * Returns the first successful match.
     *
     * @param path the request path (e.g., "/events/123")
     * @param mappings list of URI mappings to try
     * @return MatchResult if a mapping matched, empty otherwise
     */
    public Optional<MatchResult> match(String path, List<Mapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return Optional.empty();
        }

        for (var mapping : mappings) {
            var result = matchSingle(path, mapping);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    /**
     * Attempts to match a path against a single mapping.
     *
     * @param path the request path
     * @param mapping the mapping to try
     * @return MatchResult if matched, empty otherwise
     */
    private Optional<MatchResult> matchSingle(String path, Mapping mapping) {
        if (!mapping.templated()) {
            // Non-templated mapping: exact match only
            if (mapping.href().equals(path)) {
                return Optional.of(new MatchResult(mapping, Collections.emptyMap()));
            }
            return Optional.empty();
        }

        // Extract variable names from the pattern
        var variableNames = extractVariableNames(mapping.href());

        // Convert URI template to regex
        var regex = uriTemplateToRegex(mapping.href());
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(path);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        // Extract variable values
        var variables = new HashMap<String, String>();
        for (int i = 0; i < variableNames.size(); i++) {
            var value = matcher.group(i + 1); // Groups start at 1
            variables.put(variableNames.get(i), value);
        }

        return Optional.of(new MatchResult(mapping, variables));
    }

    /**
     * Extracts variable names from a URI template pattern.
     * Example: "/events/{id}/comments/{commentId}" -> ["id", "commentId"]
     */
    private List<String> extractVariableNames(String pattern) {
        var names = new ArrayList<String>();
        var matcher = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}").matcher(pattern);

        while (matcher.find()) {
            names.add(matcher.group(1));
        }

        return names;
    }

    /**
     * Converts a URI template pattern to a regular expression.
     *
     * Example: "/events/{id}" -> "^/events/([^/]+)$"
     *
     * Variables {varName} are converted to capture groups that match one or more
     * characters except forward slash.
     */
    private String uriTemplateToRegex(String pattern) {
        var regex = new StringBuilder("^");
        int i = 0;
        int length = pattern.length();

        while (i < length) {
            char c = pattern.charAt(i);

            if (c == '{') {
                // Find the closing brace
                int closingBrace = pattern.indexOf('}', i);
                if (closingBrace == -1) {
                    throw new IllegalArgumentException("Unclosed variable in pattern: " + pattern);
                }

                // Replace {varName} with a capture group
                regex.append("([^/]+)");
                i = closingBrace + 1;
            } else if (isRegexSpecialChar(c)) {
                // Escape regex special characters
                regex.append('\\').append(c);
                i++;
            } else {
                regex.append(c);
                i++;
            }
        }

        regex.append('$');
        return regex.toString();
    }

    /**
     * Checks if a character is a regex special character that needs escaping.
     */
    private boolean isRegexSpecialChar(char c) {
        return c == '.' || c == '*' || c == '+' || c == '?' ||
               c == '^' || c == '$' || c == '(' || c == ')' ||
               c == '[' || c == ']' || c == '|' || c == '\\';
    }
}
