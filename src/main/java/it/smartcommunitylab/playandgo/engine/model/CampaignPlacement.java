package it.smartcommunitylab.playandgo.engine.model;

import java.util.HashMap;
import java.util.Map;

public class CampaignPlacement {
    private boolean active;
    private Map<String, String> title = new HashMap<>();
    private Map<String, Object> configuration = new HashMap<>();
    
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public Map<String, String> getTitle() {
        return title;
    }
    public void setTitle(Map<String, String> title) {
        this.title = title;
    }
    public Map<String, Object> getConfiguration() {
        return configuration;
    }
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}
