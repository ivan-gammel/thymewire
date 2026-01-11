package pro.gammel.thymewire.core;

import com.github.resource4j.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import pro.gammel.thymewire.rendering.thymeleaf.TemplateEngineProvider;

import java.io.StringWriter;
import java.util.Map;

public class Renderer {

    private static final Logger LOG = LoggerFactory.getLogger(Renderer.class);

    private final ITemplateEngine thymeleaf;

    public Renderer(Resources resources) {
        this.thymeleaf = TemplateEngineProvider.templateEngine(resources);
    }

    public String render(String templateName, Map<String, Object> model) {
        try {
            var context = new Context();
            if (model != null) {
                context.setVariables(model);
            }
            
            var writer = new StringWriter();
            var spec = new TemplateSpec(templateName, TemplateMode.HTML);
            thymeleaf.process(spec, context, writer);
            
            LOG.debug("Rendered template: {}", templateName);
            return writer.toString();
        } catch (Exception e) {
            LOG.error("Failed to render template: {}", templateName, e);
            return createErrorPage(templateName, e);
        }
    }
    
    private String createErrorPage(String templateName, Exception error) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Template Error</title>
                <style>
                    body { font-family: monospace; margin: 20px; }
                    .error { color: red; background: #ffeeee; padding: 10px; border: 1px solid red; }
                </style>
            </head>
            <body>
                <h1>Template Rendering Error</h1>
                <p><strong>Template:</strong> %s</p>
                <div class="error">
                    <strong>Error:</strong> %s
                </div>
            </body>
            </html>
            """.formatted(templateName, error.getMessage());
    }
}