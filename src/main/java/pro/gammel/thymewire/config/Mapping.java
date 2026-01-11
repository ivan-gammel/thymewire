package pro.gammel.thymewire.config;

import java.util.List;
import java.util.regex.Pattern;

/**
 * URL mapping configuration for serving templates with parameterized URLs.
 *
 * @param href the URL pattern with template variables (e.g., "/accounts/{id}")
 * @param templated whether the URL contains template variables
 * @param template the name of the template to render
 * @param model the variable name to use for model selection (optional)
 */
public record Mapping(
    String href,
    boolean templated,
    String template,
    String model
) implements SpecBased {

    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{[a-zA-Z_][a-zA-Z0-9_]*\\}");
    private static final Pattern MODEL_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");


    @Override
    public List<String> validate(String prefix, List<String> errors) {
        if (href() == null || href.trim().isEmpty()) {
            errors.add(prefix + "href is required");
        } else {
            validateHref(href, templated, prefix, errors);
        }

        if (template == null || template.trim().isEmpty()) {
            errors.add(prefix + "template is required");
        }

        if (model != null && !MODEL_ID_PATTERN.matcher(model).matches()) {
            errors.add(prefix + "model must contain only letters, digits, underscores and hyphens");
        }
        return errors;
    }

    private void validateHref(String href, boolean templated, String prefix, List<String> errors) {
        boolean hasTemplateVars = TEMPLATE_VARIABLE_PATTERN.matcher(href).find();

        if (templated && !hasTemplateVars) {
            errors.add(prefix + "href marked as templated but contains no template variables");
        }

        if (!templated && hasTemplateVars) {
            errors.add(prefix + "href contains template variables but not marked as templated");
        }

        if (!href.startsWith("/")) {
            errors.add(prefix + "href must start with '/'");
        }
    }
}