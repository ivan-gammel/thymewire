package pro.gammel.thymewire.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gammel.thymewire.config.SiteConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Service for discovering available data models for templates.
 */
public class ModelLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ModelLoader.class);

    // Pattern to match model files: template.modelId.json or template.json
    private static final Pattern MODEL_FILE_PATTERN = Pattern.compile("^(.+?)(?:\\.([a-zA-Z0-9_-]+))?\\.json$");

    /**
     * Discovers all data models for a specific template.
     *
     * @param templateName the template name (without .html extension)
     * @param config the site configuration
     * @param baseDirectory the base directory to resolve relative paths from
     * @return list of discovered models for the template
     */
    public List<ModelInfo> discoverModels(String templateName, SiteConfig config, Path baseDirectory) {
        List<ModelInfo> models = new ArrayList<>();

        // Mirror the pages structure: templates are in config.pages(),
        // so models should be in test/pages/
        Path testBasePath = baseDirectory.resolve(config.test());
        Path testPath = testBasePath.resolve("pages");

        if (!Files.exists(testPath)) {
            LOG.debug("Test pages directory does not exist: {}", testPath);
            return models;
        }

        LOG.trace("Discovering models for template '{}' in test directory: {}", templateName, testPath);

        try (Stream<Path> paths = Files.walk(testPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".json"))
                 .forEach(modelPath -> {
                     ModelInfo model = parseModelFile(templateName, testPath, modelPath);
                     if (model != null) {
                         LOG.trace("  -> Found model '{}' at {}", model.id().isEmpty() ? "(default)" : model.id(), modelPath);
                         models.add(model);
                     }
                 });
        } catch (IOException e) {
            LOG.error("Failed to discover models in {}: {}", testPath, e.getMessage());
        }

        LOG.trace("Discovered {} models for template '{}'", models.size(), templateName);
        return models;
    }

    private ModelInfo parseModelFile(String templateName, Path testPath, Path modelPath) {
        Path relativePath = testPath.relativize(modelPath);
        String fileName = relativePath.getFileName().toString();

        Matcher matcher = MODEL_FILE_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return null;
        }

        String fileTemplateName = matcher.group(1);
        String modelId = matcher.group(2); // null for default model

        // Include directory structure in template name comparison
        Path parentPath = relativePath.getParent();
        String fullFileTemplateName = parentPath != null ?
            parentPath.toString().replace('\\', '/') + "/" + fileTemplateName :
            fileTemplateName;

        // Check if this model file corresponds to the template
        String normalizedTemplateName = normalizeTemplateName(templateName);
        String normalizedFileTemplateName = normalizeTemplateName(fullFileTemplateName);

        LOG.trace("Checking model file: {}", modelPath);
        LOG.trace("  relativePath: {}", relativePath);
        LOG.trace("  fileName: {}", fileName);
        LOG.trace("  fileTemplateName from pattern: {}", fileTemplateName);
        LOG.trace("  modelId: {}", modelId);
        LOG.trace("  fullFileTemplateName: {}", fullFileTemplateName);
        LOG.trace("  normalizedTemplateName: {}", normalizedTemplateName);
        LOG.trace("  normalizedFileTemplateName: {}", normalizedFileTemplateName);
        LOG.trace("  Match: {}", normalizedFileTemplateName.equals(normalizedTemplateName));

        if (!normalizedFileTemplateName.equals(normalizedTemplateName)) {
            return null;
        }

        boolean isDefault = modelId == null;
        String id = isDefault ? "" : modelId;

        return new ModelInfo(id, modelPath, isDefault);
    }

    private String normalizeTemplateName(String templateName) {
        // Convert path separators to forward slashes and handle directory structure
        return templateName.replace('\\', '/');
    }

    /**
     * Discovers all models in the test directory, organized by template name.
     *
     * @param config the site configuration
     * @param baseDirectory the base directory to resolve relative paths from
     * @return total count of discovered model variants
     */
    public int discoverAllModels(SiteConfig config, Path baseDirectory) {
        Path testPath = baseDirectory.resolve(config.test());
        if (!Files.exists(testPath)) {
            LOG.debug("Test directory does not exist: {}", testPath);
            return 0;
        }

        int totalModels = 0;
        try (Stream<Path> paths = Files.walk(testPath)) {
            totalModels = (int) paths.filter(Files::isRegularFile)
                                   .filter(path -> path.toString().endsWith(".json"))
                                   .count();
        } catch (IOException e) {
            LOG.error("Failed to count models in {}: {}", testPath, e.getMessage());
        }

        return totalModels;
    }
}