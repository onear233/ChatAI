package com.onear.chatai.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "agent")
public class ModelProperties {

    private List<ModelConfig> models = new ArrayList<>();
    private String activeModel;

    public List<ModelConfig> getModels() { return models; }
    public void setModels(List<ModelConfig> models) { this.models = models; }
    public String getActiveModel() { return activeModel; }
    public void setActiveModel(String activeModel) { this.activeModel = activeModel; }

    public static class ModelConfig {
        private String id;
        private String name;
        private String type;
        private String apiEndpoint;
        private String apiKey;
        private String modelName;
        private boolean enabled = true;
        private Map<String, Object> config = Map.of();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getApiEndpoint() { return apiEndpoint; }
        public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }
}
