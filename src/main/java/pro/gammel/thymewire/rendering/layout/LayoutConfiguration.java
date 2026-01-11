package pro.gammel.thymewire.rendering.layout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LayoutConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LayoutConfiguration.class);

    private final Map<String, Layout> layouts;

    @JsonCreator
    public LayoutConfiguration(@JsonUnwrapped Map<String, Layout> layouts) {
        if (layouts == null) {
            this.layouts = Collections.emptyMap();
        } else {
            this.layouts = layouts;
        }
    }

    public int size() {
        return layouts.size();
    }

    public Optional<Layout> get(String name) {
        return Optional.ofNullable(layouts.get(name));
    }


    /**
     * Returns layout name by matching path
     * @param path path to a resource, e.g. '/events/list'
     * @return name of the layout applied to this resource
     */
    public Optional<String> findByPath(String path) {
        List<String> matches = layouts.entrySet().stream()
                .filter(entry -> entry.getValue().matches(path))
                .map(Map.Entry::getKey)
                .toList();

        if (matches.size() > 1) {
            LOG.warn("Multiple layouts matched path '{}': {}. Using first match: '{}'",
                    path, matches, matches.getFirst());
        }

        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getFirst());
    }

}
