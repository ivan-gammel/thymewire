package pro.gammel.thymewire.rendering.thymeleaf;

import com.github.resource4j.resources.Resources;
import com.github.resource4j.thymeleaf3.Resource4jTemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;

import java.util.Map;

public class ComponentTemplateResolver implements ITemplateResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentTemplateResolver.class);

    private Resource4jTemplateResolver delegate;

    public ComponentTemplateResolver(Resources resources) {
        this.delegate = new Resource4jTemplateResolver(resources);
    }

    @Override
    public String getName() {
        return "ComponentTemplateResolver";
    }

    @Override
    public Integer getOrder() {
        return delegate.getOrder();
    }

    @Override
    public TemplateResolution resolveTemplate(IEngineConfiguration configuration,
                                              String ownerTemplate,
                                              String template,
                                              Map<String, Object> templateResolutionAttributes) {
        boolean component = template.endsWith("-c");
        String resolvedName = "templates/" + (component ? "components/" : template.startsWith("layouts/") ? "" : "pages/") + template + ".html";
        LOG.debug("Resolving template {} ({}) to {}", template, ownerTemplate, resolvedName);
        return delegate.resolveTemplate(configuration, ownerTemplate, resolvedName, templateResolutionAttributes);
    }
}
