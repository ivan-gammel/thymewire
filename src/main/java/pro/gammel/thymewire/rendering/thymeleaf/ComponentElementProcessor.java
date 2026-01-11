package pro.gammel.thymewire.rendering.thymeleaf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.*;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * Processes component tags like &lt;atom:button&gt;, &lt;molecule:form-field&gt;, etc.
 * Converts them to Thymeleaf fragment includes with proper parameter mapping.
 */
public class ComponentElementProcessor extends AbstractElementModelProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentElementProcessor.class);

    private static final int PRECEDENCE = 1;
    public static final String COMPONENT_NAME_PATTERN = "%s-c";
    public static final String FRAGMENT_NAME = "content";

    private final String componentType;

    public ComponentElementProcessor(String dialectPrefix) {
        super(
            TemplateMode.HTML,
            dialectPrefix,
            null, // Match any element name with this prefix
            false, // Not a prefix match
            null,  // No attribute name
            false, // Not an attribute prefix match
            PRECEDENCE
        );
        this.componentType = dialectPrefix;
    }

    @Override
    protected void doProcess(ITemplateContext context,
                             IModel model,
                             IElementModelStructureHandler structureHandler) {
        // Get the first element from the model - could be IOpenElementTag or IStandaloneElementTag
        ITemplateEvent firstEvent = model.get(0);

        IProcessableElementTag elementTag;
        if (firstEvent instanceof IOpenElementTag) {
            elementTag = (IOpenElementTag) firstEvent;
        } else if (firstEvent instanceof IStandaloneElementTag) {
            elementTag = (IStandaloneElementTag) firstEvent;
        } else {
            return; // Not an element tag we can process
        }

        String elementName = elementTag.getElementCompleteName();
        if (!elementName.startsWith(componentType)) return;
        String componentName = elementName.substring(componentType.length() + 1); // Remove "c:" prefix
        LOG.trace("Component {} found in {}. Rendering...", componentName, context.getTemplateData().getTemplate());

        // Convert component name from kebab-case to component filename
        String componentPath = String.format(COMPONENT_NAME_PATTERN, componentName.replace('.', '/'));

        // Build fragment expression with parameters
        StringBuilder fragmentExpr = new StringBuilder();
        fragmentExpr.append("~{").append(componentPath).append(" :: ").append(FRAGMENT_NAME);

        // Extract attributes into a model map
        Map<String, Object> componentModel = extractComponentModel(elementTag, context);

        if (LOG.isDebugEnabled()) {
            var entries = componentModel.entrySet().stream()
                    .map(e -> e.getKey() + ":" + (e.getValue() != null ? e.getValue().getClass().getSimpleName() : "null"))
                            .collect(joining(","));
            LOG.trace("Preparing model for component {} with data: {}", componentName, entries);
            var nulls = componentModel.entrySet().stream()
                    .filter(e -> e.getValue() == null)
                    .map(Map.Entry::getKey)
                    .collect(joining(","));
            if (!nulls.isEmpty()) {
                LOG.debug("Model for component {} contains null values {}", componentName, nulls);
            }
        }

        // Only add model parameter if component has attributes
        if (!componentModel.isEmpty()) {
            // Create a unique variable name for this component instance
            String modelVarName = "componentModel_" + componentName.replace('.', '_') + "_" + System.nanoTime();

            // Set the model as a local variable in Thymeleaf context
            structureHandler.setLocalVariable(modelVarName, componentModel);

            // Build fragment call with the model variable
            fragmentExpr.append("(__model=${").append(modelVarName).append("})");
        }
        fragmentExpr.append("}");

        // Create a new div element with th:replace attribute
        IModelFactory modelFactory = context.getModelFactory();

        // Create the replacement div with th:replace
        IStandaloneElementTag divTag = modelFactory.createStandaloneElementTag(
            "div",
            "th:replace", fragmentExpr.toString(),
            false, false
        );

        // Replace the entire model with the new div
        model.reset();
        model.add(divTag);
    }

    private Map<String, Object> extractComponentModel(IProcessableElementTag tag, ITemplateContext context) {
        Map<String, Object> model = new HashMap<>();

        for (IAttribute attribute : tag.getAllAttributes()) {
            String attrName = attribute.getAttributeCompleteName();
            String attrValue = attribute.getValue();

            // Skip Thymeleaf attributes
            if (attrName.startsWith("th:")) {
                continue;
            }

            // Convert kebab-case to camelCase
            String propertyName = kebabToCamelCase(attrName);

            // Convert attribute value to appropriate type and evaluate expressions
            Object propertyValue = convertAttributeValue(attrValue, context);

            model.put(propertyName, propertyValue);
        }

        return model;
    }

    private Object convertAttributeValue(String attrValue, ITemplateContext context) {
        if (attrValue == null || attrValue.isEmpty()) {
            return null;
        }

        // Handle boolean values
        if ("true".equals(attrValue)) {
            return Boolean.TRUE;
        }
        if ("false".equals(attrValue)) {
            return Boolean.FALSE;
        }

        // Handle Thymeleaf expressions - evaluate them now
        if (attrValue.startsWith("${") && attrValue.endsWith("}")) {
            try {
                IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
                IStandardExpression expression = parser.parseExpression(context, attrValue);
                return expression.execute(context);
            } catch (Exception e) {
                // If evaluation fails, return the original string
                return attrValue;
            }
        }
        if (attrValue.startsWith("#{") && attrValue.endsWith("}")) {
            try {
                IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
                IStandardExpression expression = parser.parseExpression(context, attrValue);
                return expression.execute(context);
            } catch (Exception e) {
                // If evaluation fails, return the original string
                return attrValue;
            }
        }

        // Handle numeric values
        try {
            // Try integer first
            return Integer.parseInt(attrValue);
        } catch (NumberFormatException e1) {
            try {
                // Try double
                return Double.parseDouble(attrValue);
            } catch (NumberFormatException e2) {
                // Default to string
                return attrValue;
            }
        }
    }


    private String kebabToCamelCase(String kebabCase) {
        if (kebabCase == null || kebabCase.isEmpty()) {
            return kebabCase;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : kebabCase.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}