package pro.gammel.thymewire.discovery;

import java.nio.file.Path;

/**
 * Information about a discovered data model.
 *
 * @param id the model identifier (empty string for default model, or specific id like "empty", "single")
 * @param path the full file system path to the model JSON file
 * @param isDefault whether this is the default model for the template
 */
public record ModelInfo(
    String id,
    Path path,
    boolean isDefault
) {}