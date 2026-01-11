package pro.gammel.thymewire.rendering.thymeleaf;

import com.github.resource4j.resources.Resources;
import com.github.resource4j.thymeleaf3.Resource4jMessageResolver;
import com.github.resource4j.thymeleaf3.Resource4jTemplateEngine;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.IContext;

import java.io.Writer;
import java.util.HashMap;

public class TemplateEngineProvider {

    public static ITemplateEngine templateEngine(Resources resources) {
        var delegate = new TemplateEngine() {
            @Override
            protected void initializeSpecific() {
                setMessageResolver(new ComponentMessageResolver(new Resource4jMessageResolver(resources)));
            }
        };
        delegate.setTemplateResolver(new ComponentTemplateResolver(resources));
        delegate.addDialect(new ComponentDialect());
        return new Resource4jTemplateEngine(delegate) {
            @Override
            public void process(TemplateSpec templateSpec, IContext context, Writer writer) {
                var attr = templateSpec.getTemplateResolutionAttributes();
                if (attr == null) {
                    attr = new HashMap<>();
                    attr.put("locale", context.getLocale());
                    templateSpec = new TemplateSpec(templateSpec.getTemplate(), templateSpec.getTemplateSelectors(), templateSpec.getTemplateMode(), attr);
                } else {
                    attr.put("locale", context.getLocale());
                }
                super.process(templateSpec, context, writer);
            }
        };
    }

}
