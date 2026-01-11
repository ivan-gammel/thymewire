package pro.gammel.thymewire.rendering.thymeleaf;

import com.github.resource4j.thymeleaf3.Resource4jMessageResolver;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.IMessageResolver;

public class ComponentMessageResolver implements IMessageResolver {

    private static final int PRECEDENCE = 100;

    private final Resource4jMessageResolver delegate;

    public ComponentMessageResolver(Resource4jMessageResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return "ComponentMessageResolver";
    }

    @Override
    public Integer getOrder() {
        return PRECEDENCE;
    }

    @Override
    public String resolveMessage(ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
        return delegate.resolveMessage(context, origin, resolve(context, origin, key), messageParameters);
    }

    private String resolve(ITemplateContext context, Class<?> origin, String key) {
        if (key.indexOf('.') < 0) {
            var template = context.getTemplateData().getTemplate();
            boolean component = template.endsWith("-c");
            return (component ? "components" : "pages") + "." + template.replace('/', '.') + "." + key;
        } else {
            return key;
        }
    }

    @Override
    public String createAbsentMessageRepresentation(ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
        return delegate.resolveMessage(context, origin, resolve(context, origin, key), messageParameters);
    }
}
