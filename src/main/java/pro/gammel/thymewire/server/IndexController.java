package pro.gammel.thymewire.server;

import com.sun.net.httpserver.HttpExchange;
import pro.gammel.thymewire.core.SiteProvider;
import pro.gammel.thymewire.views.GeneratedIndexBuilder;

public class IndexController implements Controller {

    private final GeneratedIndexBuilder builder = new GeneratedIndexBuilder();
    private final SiteProvider site;

    public IndexController(SiteProvider site) {
        this.site = site;
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean accepts(HttpExchange exchange) {
        var path = exchange.getRequestURI().getPath();
        return "/".equals(path) || path.isEmpty();
    }

    @Override
    public Response respond(HttpExchange exchange) {
        var result = builder.buildTemplateList(site.templates().values().stream().toList(), site.config());
        return new Response(200, result);
    }

}
