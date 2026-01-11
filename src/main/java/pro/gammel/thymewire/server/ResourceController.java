package pro.gammel.thymewire.server;

import com.github.resource4j.resources.Resources;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gammel.thymewire.core.SiteProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ResourceController implements Controller {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceController.class);

    private final SiteProvider site;

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
        Map.entry("css", "text/css"),
        Map.entry("js", "application/javascript"),
        Map.entry("json", "application/json"),
        Map.entry("xml", "application/xml"),
        Map.entry("pdf", "application/pdf"),
        Map.entry("png", "image/png"),
        Map.entry("jpg", "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("gif", "image/gif"),
        Map.entry("svg", "image/svg+xml"),
        Map.entry("webp", "image/webp"),
        Map.entry("ico", "image/x-icon"),
        Map.entry("woff", "font/woff"),
        Map.entry("woff2", "font/woff2"),
        Map.entry("ttf", "font/ttf"),
        Map.entry("otf", "font/otf")
    );

    private final Resources resources;

    public ResourceController(SiteProvider site, Resources resources) {
        this.site = site;
        this.resources = resources;
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean accepts(HttpExchange exchange) {
        var path = exchange.getRequestURI().getPath();

        // Don't handle root path or paths without extension
        if ("/".equals(path) || path.isEmpty()) {
            return false;
        }

        // Don't handle HTML files
        if (path.endsWith(".html")) {
            return false;
        }

        // Accept files with extensions that exist in resources directory
        var extension = fileExtension(path);
        if (extension.isEmpty()) {
            return false;
        }

        var resourcePath = resolveResourcePath(path);
        boolean exists = Files.exists(resourcePath) && Files.isRegularFile(resourcePath);
        LOG.debug("Path {} {}exist", resourcePath, exists ? "" : "does not ");
        return exists;
    }

    @Override
    public Response respond(HttpExchange exchange) {
        var path = exchange.getRequestURI().getPath();
        var resourcePath = resolveResourcePath(path);

        try {
            var content = Files.readAllBytes(resourcePath);
            var mimeType = mimeType(path);

            LOG.debug("Serving resource: {} ({})", path, mimeType);
            return new Response(200, content, mimeType);

        } catch (SecurityException e) {
            LOG.warn("Security violation: {}", e.getMessage());
            return new Response(403, "Forbidden");
        } catch (IOException e) {
            LOG.warn("Failed to read resource file {}: {}", resourcePath, e.getMessage());
            return new Response(404, "Resource not found");
        }
    }

    /**
     * Resolves resource path with security validation to prevent directory traversal.
     * The resolved path is normalized and validated to be within the resources directory.
     *
     * @param requestPath the requested resource path
     * @return the resolved path
     * @throws SecurityException if path attempts to escape resources directory
     */
    private Path resolveResourcePath(String requestPath) {
        var config = site.config();
        var basePath = site.basePath().toAbsolutePath();
        var resourcesPath = basePath.resolve(config.resources()).normalize();

        // Remove leading slash and resolve relative to resources directory
        var relativePath = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        var result = resourcesPath.resolve(relativePath).normalize();

        // Security check: ensure resolved path is within resources directory
        if (!result.startsWith(resourcesPath)) {
            LOG.warn("Path traversal attempt blocked: {}", requestPath);
            throw new SecurityException("Invalid resource path");
        }

        LOG.info("Resolving resource path: {}", result);
        return result;
    }

    private String fileExtension(String path) {
        var lastDot = path.lastIndexOf('.');
        if (lastDot == -1 || lastDot == path.length() - 1) {
            return "";
        }
        return path.substring(lastDot + 1).toLowerCase();
    }

    private String mimeType(String path) {
        var extension = fileExtension(path);
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }
}