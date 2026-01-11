package pro.gammel.thymewire.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gammel.thymewire.config.SiteConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for discovering available templates in the configured source directories.
 */
public class TemplateLoader {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateLoader.class);

    private final ModelLoader modelDiscovery;

    public TemplateLoader(ModelLoader modelDiscovery) {
        this.modelDiscovery = modelDiscovery;
    }

    /**
     * Discovers all templates based on the site configuration.
     *
     * @param config the site configuration
     * @param baseDirectory the base directory to resolve relative paths from
     * @return list of discovered templates
     */
    public List<TemplateInfo> discoverTemplates(SiteConfig config, Path baseDirectory) {
        List<TemplateInfo> templates = new ArrayList<>();

        Path srcPath = baseDirectory.resolve(config.pages());
        if (Files.exists(srcPath)) {
            templates.addAll(discoverTemplatesInDirectory(srcPath, config, baseDirectory));
        } else {
            LOG.warn("Template source directory does not exist: {}", srcPath);
        }

        LOG.info("Discovered {} templates", templates.size());
        return templates;
    }

    private List<TemplateInfo> discoverTemplatesInDirectory(Path srcPath, SiteConfig config, Path baseDirectory) {
        List<TemplateInfo> templates = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(srcPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".html"))
                 .forEach(templatePath -> {
                     String templateName = getTemplateName(srcPath, templatePath);
                     List<ModelInfo> models = modelDiscovery.discoverModels(templateName, config, baseDirectory);
                     templates.add(new TemplateInfo(templateName, templatePath, models));
                 });
        } catch (IOException e) {
            LOG.error("Failed to discover templates in {}: {}", srcPath, e.getMessage());
        }

        return templates;
    }

    private String getTemplateName(Path srcPath, Path templatePath) {
        Path relativePath = srcPath.relativize(templatePath);
        String pathString = relativePath.toString().replace('\\', '/');

        // Remove .html extension
        if (pathString.endsWith(".html")) {
            pathString = pathString.substring(0, pathString.length() - 5);
        }

        return pathString;
    }
}