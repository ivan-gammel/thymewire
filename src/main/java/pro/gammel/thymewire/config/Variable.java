package pro.gammel.thymewire.config;

import java.util.List;

public record Variable(String name) implements SpecBased {

    @Override
    public List<String> validate(String property, List<String> errors) {
        if (name == null || name.isEmpty()) {
            errors.add(property + " is required");
        } else if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            errors.add(property + " must be a valid variable name, was '" + name + "'");
        }
        return errors;
    }

}
