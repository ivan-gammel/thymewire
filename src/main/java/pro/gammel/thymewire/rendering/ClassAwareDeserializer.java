package pro.gammel.thymewire.rendering;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Deserializer that handles "class" meta field in JSON to deserialize objects into specific types.
 * When a property of the "model" object contains a "class" field, it will be deserialized as an instance of that class.
 */
public class ClassAwareDeserializer {
    private static final Logger LOG = LoggerFactory.getLogger(ClassAwareDeserializer.class);
    private static final String CLASS_META_FIELD = "class";

    private final ObjectMapper mapper;

    public ClassAwareDeserializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Processes properties of the model map, deserializing those with "class" meta field.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> processModel(Map<String, Object> model) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : model.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) value;
                String className = (String) valueMap.get(CLASS_META_FIELD);

                if (className != null) {
                    try {
                        // Load the class
                        Class<?> targetClass = Class.forName(className);

                        // Create a new map without the "class" field
                        Map<String, Object> dataMap = new HashMap<>(valueMap);
                        dataMap.remove(CLASS_META_FIELD);

                        // Deserialize to the specified class
                        Object typedObject = mapper.convertValue(dataMap, targetClass);
                        LOG.debug("Deserialized model property '{}' as {}", key, targetClass.getSimpleName());
                        result.put(key, typedObject);
                        continue;
                    } catch (ClassNotFoundException e) {
                        LOG.warn("Class not found: {}, keeping property '{}' as Map", className, key);
                    } catch (Exception e) {
                        LOG.warn("Failed to deserialize property '{}' into class {}: {}", key, className, e.getMessage());
                    }
                }
            }

            // No class field or deserialization failed, keep original value
            result.put(key, value);
        }

        return result;
    }
}
