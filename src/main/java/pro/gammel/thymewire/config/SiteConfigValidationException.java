package pro.gammel.thymewire.config;

import java.util.List;

/**
 * Exception thrown when site configuration validation fails.
 */
public class SiteConfigValidationException extends Exception {
    private final List<String> validationErrors;

    public SiteConfigValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = List.copyOf(validationErrors);
    }

    public List<String> validationErrors() {
        return validationErrors;
    }
}