package pro.gammel.thymewire.config;

import java.util.List;

public interface SpecBased {



    /**
     * Validate current instance of this interface and append errors to given list
     * @param property the name of the property referencing current object (prefix of the error)
     * @param errors the list of errors to append to
     * @return errors
     */
    List<String> validate(String property, List<String> errors);

}
