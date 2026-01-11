package pro.gammel.thymewire.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parser for site.json configuration files.
 */
public class SiteConfigParser {
    private static final Logger LOG = LoggerFactory.getLogger(SiteConfigParser.class);

    private final ObjectMapper mapper;

    public SiteConfigParser() {
        this.mapper = new ObjectMapper();
    }

    /**
     * Loads site configuration from the specified file path.
     * If the file doesn't exist or parsing fails, returns default configuration.
     *
     * @param configPath the path to the site.json file
     * @return the parsed site configuration or default configuration if loading fails
     */
    public SiteConfig loadConfig(Path configPath) {
        if (!Files.exists(configPath)) {
            LOG.warn("Site configuration file not found at {}, using defaults", configPath);
            return SiteConfig.defaultConfig();
        }

        try {
            String content = Files.readString(configPath);
            SiteConfig config = mapper.readValue(content, SiteConfig.class);
            LOG.debug("Site configuration loaded from {}", configPath);
            return config;
        } catch (IOException e) {
            LOG.warn("Failed to parse site configuration from {}: {}, using defaults",
                    configPath, e.getMessage());
            return SiteConfig.defaultConfig();
        }
    }

    /**
     * Loads site configuration from site.json in the specified directory.
     *
     * @param launchDirectory the directory to search for site.json
     * @return the parsed site configuration or default configuration if loading fails
     */
    public SiteConfig loadConfigFromDirectory(Path launchDirectory) {
        Path configPath = launchDirectory.resolve("site.json");
        return loadConfig(configPath);
    }
}