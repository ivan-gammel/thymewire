package pro.gammel.thymewire.rendering.thymeleaf;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.HashSet;
import java.util.Set;

/**
 * Configurable Thymeleaf dialect for atomic design components.
 * Can be instantiated with different prefixes (atom, molecule, organism)
 * to enable usage like: &lt;atom:button /&gt;, &lt;molecule:form-field /&gt;, &lt;organism:navigation /&gt;
 */
public class ComponentDialect extends AbstractProcessorDialect {

    public static final int PROCESSOR_PRECEDENCE = 1;

    public ComponentDialect() {
        super("Components",
              "c",
              PROCESSOR_PRECEDENCE);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        Set<IProcessor> processors = new HashSet<>();
        processors.add(new ComponentElementProcessor(dialectPrefix));
        return processors;
    }
}