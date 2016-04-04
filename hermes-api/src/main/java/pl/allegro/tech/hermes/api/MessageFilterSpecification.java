package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

public class MessageFilterSpecification {
    private String type;
    private Map<String, Object> spec;

    public MessageFilterSpecification() {
    }

    @JsonCreator
    public MessageFilterSpecification(Map<String, Object> spec) {
        this.spec = spec;
        this.type = getStringValue("type");
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return getStringValue("path");
    }

    public String getMatcher() {
        return getStringValue("matcher");
    }

    @SuppressWarnings("unchecked")
    public <T> T getFieldValue(String key) {
        return (T) spec.get(key);
    }

    public String getStringValue(String key) {
        return getFieldValue(key);
    }

    @JsonValue
    public Object getJsonValue() {
        return spec;
    }
}
