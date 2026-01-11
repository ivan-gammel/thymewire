package pro.gammel.thymewire.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.resource4j.resources.Resources;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gammel.thymewire.config.SiteConfig;
import pro.gammel.thymewire.core.Renderer;
import pro.gammel.thymewire.core.SiteProvider;
import pro.gammel.thymewire.core.UriTemplateMatcher;
import pro.gammel.thymewire.rendering.ClassAwareDeserializer;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TemplateController implements Controller {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateController.class);

    private final SiteProvider site;
    private final Renderer renderer;
    private final ObjectMapper mapper;
    private final ClassAwareDeserializer deserializer;
    private final UriTemplateMatcher uriMatcher;

    public TemplateController(SiteProvider site, Resources resources) {
        this.site = site;
        this.renderer = new Renderer(resources);
        this.mapper = new ObjectMapper().findAndRegisterModules();
        this.deserializer = new ClassAwareDeserializer(mapper);
        this.uriMatcher = new UriTemplateMatcher();
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean accepts(HttpExchange exchange) {
        var path = exchange.getRequestURI().getPath();

        // Null safety checks
        if (site.config() == null || site.templates() == null) {
            return false;
        }

        // First try exact template match
        var templateName = resolveTemplateName(path);
        if (site.templates().containsKey(templateName)) {
            return true;
        }

        // Try URI template matching against mappings
        var mappings = site.config().mappings();
        if (mappings != null && !mappings.isEmpty()) {
            var matchResult = uriMatcher.match(path, mappings);
            if (matchResult.isPresent()) {
                var template = matchResult.get().templateName();
                return site.templates().containsKey(template);
            }
        }

        return false;
    }

    @Override
    public Response respond(HttpExchange exchange) {
        var method = exchange.getRequestMethod();
        var path = exchange.getRequestURI().getPath();

        // Extract query parameters first to check for model selection
        var queryParams = extractQueryParameters(exchange);

        // Try to match against URI templates
        var matchResult = matchUriTemplate(path);
        String templateName;
        String modelId = null;
        Map<String, String> uriVariables = new HashMap<>();

        if (matchResult.isPresent()) {
            // Use template name from mapping
            templateName = matchResult.get().templateName();
            uriVariables = matchResult.get().variables();
            modelId = matchResult.get().modelId().orElse(null);
            LOG.info("Matched URI template for path '{}': template='{}', variables={}, modelId='{}'",
                    path, templateName, uriVariables, modelId);
        } else {
            // Fall back to direct path resolution
            templateName = resolveTemplateName(path);
        }

        // Load raw model with form metadata
        var rawModel = loadRawModel(templateName, queryParams, modelId);

        // Handle form submissions (POST, PUT, PATCH)
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            return handleFormSubmission(rawModel, queryParams, path, templateName);
        }

        // For GET requests, render the page
        return renderPage(templateName, rawModel, queryParams, uriVariables, path);
    }

    private Response handleFormSubmission(Map<String, Object> rawModel, Map<String, Object> queryParams,
                                         String currentPath, String templateName) {
        LOG.info("Form submission for template '{}' at path '{}'", templateName, currentPath);
        LOG.info("Raw model: {}", rawModel);

        // Check if form metadata exists
        var formConfig = (Map<String, Object>) rawModel.get("form");
        if (formConfig == null) {
            // No form config, re-render current page with model data
            LOG.warn("Form submission: no form config found in raw model, re-rendering current page");
            return renderPage(templateName, rawModel, queryParams, currentPath);
        }

        LOG.info("Form config found: {}", formConfig);

        // Check if action is specified
        var action = (String) formConfig.get("action");
        if (action == null || action.isEmpty()) {
            // No action specified, re-render current page
            LOG.warn("Form submission: no action specified in form config, re-rendering current page");
            return renderPage(templateName, rawModel, queryParams, currentPath);
        }

        // Build redirect URL with optional model parameter
        var redirectUrl = action;
        var targetModel = (String) formConfig.get("model");
        if (targetModel != null && !targetModel.isEmpty()) {
            // Add model selection parameter
            var modelSelector = site.config().modelSelector() != null ? site.config().modelSelector() : SiteConfig.DEFAULT_MODEL_SELECTOR;
            var separator = action.contains("?") ? "&" : "?";
            redirectUrl = action + separator + modelSelector + "=" + targetModel;
        }

        LOG.info("Form submission: redirecting to '{}'", redirectUrl);
        return Response.redirect(redirectUrl);
    }

    /**
     * Renders a page with the given template and model, applying layout if configured.
     * This is the central rendering method used for both GET requests and form re-rendering.
     */
    private Response renderPage(String templateName, Map<String, Object> rawModel,
                                Map<String, Object> queryParams, Map<String, String> uriVariables, String path) {
        // Extract the model data from raw model
        var templateModel = extractTemplateModel(rawModel);

        // Add URI variables to template model (from path parameters like {id})
        templateModel.putAll(uriVariables);

        // Query params override template model and URI variables
        templateModel.putAll(queryParams);

        // Check if layout should be applied
        var layoutName = findMatchingLayout(path);
        if (layoutName != null) {
            LOG.info("Found layout '{}' for path '{}'", layoutName, path);
            return renderWithLayout(templateName, templateModel, layoutName);
        } else {
            var html = renderer.render(templateName, templateModel);
            return new Response(200, html);
        }
    }

    /**
     * Overload for backward compatibility (form submissions don't have URI variables).
     */
    private Response renderPage(String templateName, Map<String, Object> rawModel,
                                Map<String, Object> queryParams, String path) {
        return renderPage(templateName, rawModel, queryParams, new HashMap<>(), path);
    }

    private Map<String, Object> extractTemplateModel(Map<String, Object> rawModel) {
        // Check if the model follows the new structure with "model" wrapper
        if (rawModel.containsKey("model")) {
            var modelData = rawModel.get("model");
            if (modelData instanceof Map) {
                // Process model properties to deserialize those with "class" meta field
                return deserializer.processModel((Map<String, Object>) modelData);
            }
        }
        // Fallback to using the entire raw model (backward compatibility)
        return new HashMap<>(rawModel);
    }

    /**
     * Attempts to match a path against URI template mappings.
     */
    private Optional<UriTemplateMatcher.MatchResult> matchUriTemplate(String path) {
        var mappings = site.config().mappings();
        if (mappings == null || mappings.isEmpty()) {
            return Optional.empty();
        }
        return uriMatcher.match(path, mappings);
    }

    private String resolveTemplateName(String path) {
        if (path.equals("/") || path.isEmpty()) {
            return "index";
        }

        // Remove leading slash and use path as template name
        var templateName = path.startsWith("/") ? path.substring(1) : path;

        // Remove trailing slash if present
        if (templateName.endsWith("/")) {
            templateName = templateName.substring(0, templateName.length() - 1);
        }

        return templateName;
    }

    private Map<String, Object> extractQueryParameters(HttpExchange exchange) {
        var queryParams = new HashMap<String, Object>();
        var query = exchange.getRequestURI().getQuery();

        if (query != null) {
            var pairs = query.split("&");
            for (var pair : pairs) {
                var keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    // URL decode both key and value to handle encoded characters
                    var decodedKey = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    var decodedValue = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    queryParams.put(decodedKey, decodedValue);
                }
            }
        }

        return queryParams;
    }

    private String findMatchingLayout(String path) {
        var layoutConfiguration = site.layoutConfiguration();
        if (layoutConfiguration == null) {
            return null;
        }

        return layoutConfiguration.findByPath(path).orElse(null);
    }

    private Response renderWithLayout(String templateName, Map<String, Object> templateModel, String layoutName) {
        // Load layout model
        var layoutModel = loadModel("layouts/" + layoutName);

        // Start with layout model as base
        var mergedModel = new HashMap<>(layoutModel);

        // Merge template model, overwriting layout values
        mergedModel.putAll(templateModel);

        // Add page variable pointing to the original template
        mergedModel.put("page", templateName);

        // Render layout template with merged model
        var html = renderer.render("layouts/" + layoutName, mergedModel);
        return new Response(200, html);
    }

    private Map<String, Object> loadRawModel(String templateName, Map<String, Object> queryParams, String uriModelId) {
        try {
            var config = site.config();
            var basePath = site.basePath();

            // Determine which model to load (priority: query param > URI model ID > default)
            var modelSelector = config.modelSelector() != null ? config.modelSelector() : SiteConfig.DEFAULT_MODEL_SELECTOR;
            var selectedModel = (String) queryParams.get(modelSelector);

            // If no query param model, use URI-based model ID from mapping
            if ((selectedModel == null || selectedModel.isEmpty()) && uriModelId != null && !uriModelId.isEmpty()) {
                selectedModel = uriModelId;
                LOG.info("Using model ID from URI template: '{}'", selectedModel);
            }

            // Validate that the requested model exists (using original template name)
            if (selectedModel != null && !selectedModel.isEmpty()) {
                if (!site.modelExists(templateName, selectedModel)) {
                    LOG.warn("Requested model '{}' does not exist for template '{}', using default", selectedModel, templateName);
                    selectedModel = null; // Reset to use default
                }
            }

            // Add "pages/" prefix to template name for file path construction only
            var modelPath = templateName.startsWith("layouts/") ? templateName : "pages/" + templateName;

            Path testPath;
            if (selectedModel != null && !selectedModel.isEmpty()) {
                // Load specific model variant
                testPath = basePath.resolve(config.test()).resolve(modelPath + "." + selectedModel + ".json");
                LOG.info("Looking for specific model file: {}", testPath);
            } else {
                // Load default model
                testPath = basePath.resolve(config.test()).resolve(modelPath + ".json");
                LOG.info("Looking for default model file: {}", testPath);
            }

            if (Files.exists(testPath)) {
                var content = Files.readString(testPath);
                var model = mapper.readValue(content, HashMap.class);
                LOG.info("Loaded raw model for template '{}': {}", templateName, model);
                return model;
            } else {
                LOG.warn("Model file not found: {}", testPath);
            }
        } catch (IOException e) {
            LOG.warn("Failed to load model for template '{}': {}", templateName, e.getMessage());
        }

        return new HashMap<>();
    }

    private Map<String, Object> loadRawModel(String templateName, Map<String, Object> queryParams) {
        return loadRawModel(templateName, queryParams, null);
    }

    private Map<String, Object> loadRawModel(String templateName) {
        return loadRawModel(templateName, new HashMap<>(), null);
    }

    private Map<String, Object> loadModel(String templateName, Map<String, Object> queryParams) {
        var rawModel = loadRawModel(templateName, queryParams);
        return extractTemplateModel(rawModel);
    }

    private Map<String, Object> loadModel(String templateName) {
        return loadModel(templateName, new HashMap<>());
    }
}
