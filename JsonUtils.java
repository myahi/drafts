import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility class for JSON serialization / deserialization using Jackson.
 */
public final class JsonUtils {

    // Single reusable ObjectMapper (thread-safe after configuration)
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // Do not serialize null fields
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Optional: write date/time in ISO-8601 instead of timestamps
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JsonUtils() {
        // prevent instantiation
    }

    /**
     * Serialize a POJO to compact JSON string.
     */
    public static String toJson(Object value) {
            try {
                return MAPPER.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error while serializing object to JSON", e);
            }
    }

    /**
     * Serialize a POJO to pretty-printed JSON (useful for logs / audit UI).
     */
    public static String toJsonPretty(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter()
                         .writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while serializing object to JSON (pretty)", e);
        }
    }

    /**
     * Deserialize JSON string into a POJO of given type.
     */
    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Error while deserializing JSON to object", e);
        }
    }

    /**
     * Expose mapper if you need advanced config elsewhere.
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
