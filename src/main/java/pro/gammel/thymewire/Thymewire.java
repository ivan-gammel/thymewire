package pro.gammel.thymewire;

import com.github.resource4j.resources.RefreshableResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gammel.thymewire.core.SiteProvider;
import pro.gammel.thymewire.server.IndexController;
import pro.gammel.thymewire.server.ResourceController;
import pro.gammel.thymewire.server.TemplateController;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.resource4j.objects.providers.ResourceObjectProviders.bind;
import static com.github.resource4j.objects.providers.ResourceObjectProviders.filesIn;
import static com.github.resource4j.objects.providers.resolvers.DefaultObjectNameResolver.javaPropertiesLocaleResolver;
import static com.github.resource4j.resources.ResourcesConfigurationBuilder.configure;
import static com.github.resource4j.resources.cache.Caches.never;
import static com.github.resource4j.resources.processors.BasicValuePostProcessor.macroSubstitution;
import static pro.gammel.thymewire.server.PreviewerServerBuilder.aPreviewServer;

/**
 * Main application class for the Thymewire.
 */
public class Thymewire {

    private static final Logger LOG = LoggerFactory.getLogger(Thymewire.class);

    private final SiteProvider site;


    public Thymewire(Path launchDirectory) {
        this.site = new SiteProvider(launchDirectory);
    }

    /**
     * Main entry point for the application.
     *
     * @param args command-line arguments:
     *             --port <port>  Port number (default: 8085)
     *             --dir <path>   Launch directory (default: current directory)
     */
    public static void main(String[] args) {
        Path launchDirectory = Paths.get(".");
        int port = 8085; // default port

        // Parse command-line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                    i++; // skip next argument
                } catch (NumberFormatException e) {
                    LOG.error("Invalid port number: {}", args[i + 1]);
                    System.exit(1);
                }
            } else if (args[i].equals("--dir") && i + 1 < args.length) {
                launchDirectory = Paths.get(args[i + 1]);
                i++; // skip next argument
            } else if (!args[i].startsWith("--")) {
                // For backward compatibility, treat first non-option argument as directory
                launchDirectory = Paths.get(args[i]);
            }
        }

        Thymewire app = new Thymewire(launchDirectory);
        app.start(port);
    }

    /**
     * Starts the previewer server.
     *
     * @param port the port to run the server on
     */
    public void start(int port) {
        this.site.load();
        var config =  site.config();
        var basePath = filesIn(this.site.basePath().toFile()).with(javaPropertiesLocaleResolver());
        var configuration = configure()
                .sources(bind(basePath).to(config.src()),
                         bind(basePath).to(config.test()))
                .cacheObjects(never())
                .cacheBundles(never())
                .cacheValues(never())
                .postProcessingBy(macroSubstitution())
                .get();
        var resources = new RefreshableResources(configuration);

        LOG.info("Running Thymewire server on http://localhost:{}", port);
        var defaultController = new TemplateController(site, resources);
        var indexController = new IndexController(site);
        var resourceController = new ResourceController(site, resources);
        var server = aPreviewServer().on(port).serve(defaultController, indexController, resourceController).build();
        server.start();
    }

}