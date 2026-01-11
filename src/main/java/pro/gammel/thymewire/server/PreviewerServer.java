package pro.gammel.thymewire.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class PreviewerServer implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewerServer.class);

    private final int port;

    private final String host;

    private final Set<Controller> controllers;

    public PreviewerServer(String host, int port, Set<Controller> controllers) {
        this.host = host;
        this.port = port;
        this.controllers = controllers;
    }
    
    public void start() {
        try {
            var server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/", this);
            server.setExecutor(null);
            server.start();
            
            LOG.info("Running Thymewire previewer on http://{}:{}", host, port);
        } catch (IOException e) {
            LOG.error("Failed to start server", e);
            throw new RuntimeException("Failed to start server", e);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var method = exchange.getRequestMethod();
        var path = exchange.getRequestURI().getPath();


        var response = Response.defaultResponse();

        if ("GET".equals(method) || "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            try {
                for (Controller controller : controllers) {
                    if (controller.accepts(exchange)) {
                        response = controller.respond(exchange);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error handling request: {}", path, e);
                response = new Response(500, "Internal Server Error");
            }
        } else {
            response = new Response(405, "Method not allowed");
        }

        if (response.statusCode() > 299) {
            LOG.warn("{} {} -> HTTP {}", method, path, response.statusCode());
        } else {
            LOG.info("{} {} -> HTTP {} / Content-Type: {}", method, path, response.statusCode(), response.contentType());
        }

        // Handle redirects
        if (response.isRedirect()) {
            exchange.getResponseHeaders().set("Location", response.redirectLocation());
            exchange.sendResponseHeaders(response.statusCode(), -1);
            return;
        }

        var responseBytes = response.binaryContent() != null ? response.binaryContent() : response.body().getBytes(StandardCharsets.UTF_8);
        var contentType = response.contentType() != null ? response.contentType() : "text/html; charset=utf-8";
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(response.statusCode(), responseBytes.length);
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

}