package pro.gammel.thymewire.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Site configuration loaded from site.json file.
 *
 * @param index the starting page template name (default: "index.html")
 * @param src the location of templates (default: "src/main/resources/templates")
 * @param resources the location of assets like CSS, JS, images (default: same as src)
 * @param test the location of template models (default: "src/test/resources/templates")
 * @param mappings list of URL mapping configurations
 * @param modelSelector the query parameter name for model selection (default: "__preview_model")
 */
public record SiteConfig(
    String index,
    String src,
    String pages,
    String messages,
    String resources,
    String test,
    List<Mapping> mappings,
    @JsonProperty("model_selector") String modelSelector
) implements SpecBased {

    /**
     * Default query parameter name for model selection.
     */
    public static final String DEFAULT_MODEL_SELECTOR = "__preview_model";

    public String src() {
        return src != null ? src : "src/main/resources";
    }

    public String messages() {
        return messages != null ? messages : "src/main/resources";
    }

    public String resources() {
        return resources != null ?  resources : (src != null ? src : "src/main/resources/static");
    }

    public String test() {
        return  test != null ? test : "src/test/resources/templates";
    }

    /**
     * Creates a default site configuration with standard values.
     */
    public static SiteConfig defaultConfig() {
        return new SiteConfig(
            null,
            null,
            null,
            null,
            null, // resources defaults to same as src
            null,
            List.of(),
            DEFAULT_MODEL_SELECTOR
        );
    }

    /**
     * Returns the resources path, using src path as default if not specified.
     */
    public String resourcesPath() {
        return resources != null ? resources : src();
    }

    public String pages() {
        return pages != null ? pages : src() + "/templates/pages";
    }

    @Override
    public List<String> validate(String property, List<String> errors) {
        if (modelSelector != null) {
            new Variable(modelSelector).validate("model_selector", errors);
        }
        if (mappings != null) {
            for (var i = 0; i < mappings.size(); i++) {
                var mapping = mappings.get(i);
                mapping.validate("mappings[" + i + "]", errors);
            }
        }
        return errors;
    }

}