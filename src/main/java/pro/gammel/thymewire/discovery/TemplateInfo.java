package pro.gammel.thymewire.discovery;

import java.nio.file.Path;
import java.util.List;

/**
 * Information about a discovered template.
 *
 * @param name the template name (relative path without .html extension)
 * @param path the full file system path to the template
 * @param models list of available data models for this template
 */
public record TemplateInfo(
    String name,
    Path path,
    List<ModelInfo> models
) {}