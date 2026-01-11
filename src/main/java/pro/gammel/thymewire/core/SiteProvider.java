package pro.gammel.thymewire.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gammel.thymewire.config.SiteConfig;
import pro.gammel.thymewire.config.SiteConfigParser;
import pro.gammel.thymewire.discovery.ModelLoader;
import pro.gammel.thymewire.discovery.TemplateInfo;
import pro.gammel.thymewire.discovery.TemplateLoader;
import pro.gammel.thymewire.rendering.layout.LayoutConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SiteProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SiteProvider.class);

    public interface Listener {
        default void configUpdated(SiteConfig config) { refreshRequired(); }
        default void templateUpdated(String template) { refreshRequired(); }
        default void modelUpdated(String template, String model) { refreshRequired(); }
        default void refreshRequired() {}

    }

    private final Path path;

    private SiteConfig config;

    private Map<String, TemplateInfo> templates;

    private LayoutConfiguration layoutConfiguration;

    private final Set<Listener> listeners = Collections.synchronizedSet(new HashSet<>());

    public SiteProvider(Path path) {
        this.path = path;
    }

    public SiteConfig config() {
        return this.config;
    }

    public Map<String, TemplateInfo> templates() {
        return Collections.unmodifiableMap(this.templates);
    }

    public LayoutConfiguration layoutConfiguration() {
        return this.layoutConfiguration;
    }

    public Path basePath() {
        return this.path;
    }

    public void load() {
        LOG.info("Analyzing project at {}", path.toAbsolutePath());

        // Load site configuration
        config = new SiteConfigParser().loadConfigFromDirectory(path);
        if (config != null) {
            LOG.info("Site configuration found");
        } else {
            config = SiteConfig.defaultConfig();
        }

        // Validate configuration
        var errors = config.validate("", new ArrayList<>());
        if (!errors.isEmpty()) {
            LOG.warn("Site configuration validation failed with {} errors", errors.size());
            errors.forEach(error -> LOG.warn("Validation error: {}", error));
        } else {
            LOG.debug("Site configuration validation passed");
        }
        var modelLoader = new ModelLoader();
        // Discover templates and models
        templates = new HashMap<>();
        int totalModels = 0;
        for (var template : new TemplateLoader(modelLoader).discoverTemplates(config, path)) {
            LOG.trace("Registering template '{}' with {} models", template.name(), template.models().size());
            template.models().forEach(model ->
                LOG.trace("  - model id: '{}'", model.id().isEmpty() ? "(default)" : model.id())
            );
            templates.put(template.name(), template);
            totalModels += template.models().size();
        }

        LOG.info("- {} templates discovered with {} model variants", templates.size(), totalModels);

        // Load layout configuration from ${config.src}/layouts/index.json
        layoutConfiguration = loadLayoutConfiguration();
        if (layoutConfiguration != null && layoutConfiguration.size() > 0) {
            LOG.info("- {} layouts discovered", layoutConfiguration.size());
        }
    }

    private LayoutConfiguration loadLayoutConfiguration() {
        var layoutIndexPath = path.resolve(config.src()).resolve("templates/layouts/index.json");

        if (!Files.exists(layoutIndexPath)) {
            LOG.debug("Layout configuration not found at {}", layoutIndexPath);
            return new LayoutConfiguration(Collections.emptyMap());
        }

        try {
            LOG.info("Loading layout configuration from {}", layoutIndexPath);
            var mapper = new ObjectMapper();
            return mapper.readValue(layoutIndexPath.toFile(), LayoutConfiguration.class);
        } catch (IOException e) {
            LOG.error("Failed to load layout configuration from {}", layoutIndexPath, e);
            return new LayoutConfiguration(Collections.emptyMap());
        }
    }

    public void fileUpdated(Path path) {
    }

    public void subscribe(Listener listener) {
        this.listeners.add(listener);
    }

    public void unsubscribe(Listener listener) {
        this.listeners.remove(listener);
    }

    public boolean modelExists(String templateName, String modelId) {
        var templateInfo = templates.get(templateName);
        if (templateInfo == null) {
            return false;
        }
        return templateInfo.models().stream()
            .anyMatch(model -> modelId.equals(model.id()));
    }

}
