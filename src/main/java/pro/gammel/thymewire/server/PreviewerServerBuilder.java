package pro.gammel.thymewire.server;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class PreviewerServerBuilder {

    public static final int DEFAULT_PORT = 8085;
    public static final String DEFAULT_HOST = "localhost";

    private String host = DEFAULT_HOST;

    private int port = DEFAULT_PORT;

    private final Set<Controller> controllers = new HashSet<>();

    public static PreviewerServerBuilder aPreviewServer() {
        return new PreviewerServerBuilder();
    }

    public PreviewerServerBuilder on(int port) {
        this.port = port;
        return this;
    }

    public PreviewerServer build() {
        return new PreviewerServer(host, port, controllers);
    }

    public PreviewerServerBuilder serve(Controller... controllers) {
        this.controllers.addAll(asList(controllers));
        return this;
    }
}
