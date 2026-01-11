package pro.gammel.thymewire.views;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import pro.gammel.thymewire.config.SiteConfig;
import pro.gammel.thymewire.discovery.TemplateInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Service for building generated index pages using Thymeleaf templates.
 */
public class GeneratedIndexBuilder {

    private final TemplateEngine templateEngine;

    public GeneratedIndexBuilder() {
        this.templateEngine = createTemplateEngine();
    }

    /**
     * Builds an HTML page listing all discovered templates using Thymeleaf.
     *
     * @param templates list of discovered templates
     * @param config the site configuration
     * @return HTML content for the generated index page
     */
    public String buildTemplateList(List<TemplateInfo> templates, SiteConfig config) {
        Context context = new Context();

        // Build tree structure from flat template list
        TemplateTree tree = buildTemplateTree(templates);

        context.setVariable("tree", tree);
        context.setVariable("config", config);
        context.setVariable("modelSelector", config.modelSelector() != null ? config.modelSelector() : SiteConfig.DEFAULT_MODEL_SELECTOR);

        return templateEngine.process("generated-index", context);
    }

    private TemplateTree buildTemplateTree(List<TemplateInfo> templates) {
        TemplateTree root = new TemplateTree("");

        for (TemplateInfo template : templates) {
            String[] parts = template.name().split("/");
            TemplateTree current = root;

            // Navigate/create path
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                current = current.children.computeIfAbsent(part, k -> new TemplateTree(part));
            }

            // Add template at the leaf
            String leafName = parts[parts.length - 1];
            TemplateTree leaf = current.children.computeIfAbsent(leafName, k -> new TemplateTree(leafName));
            leaf.template = template;
        }

        return root;
    }

    private TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);

        return engine;
    }

    /**
     * Tree node representing a folder or template in the hierarchy.
     */
    public static class TemplateTree {
        public final String name;
        public final Map<String, TemplateTree> children = new TreeMap<>();
        public TemplateInfo template;

        public TemplateTree(String name) {
            this.name = name;
        }

        public boolean isFolder() {
            return template == null;
        }

        public boolean isTemplate() {
            return template != null;
        }

        public Collection<TemplateTree> children() {
            return children.values();
        }

        public String templateName() {
            return template != null ? template.name() : null;
        }

        public TemplateInfo templateInfo() {
            return template;
        }
    }
}