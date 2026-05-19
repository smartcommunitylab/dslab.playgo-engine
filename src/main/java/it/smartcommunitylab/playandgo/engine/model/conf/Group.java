package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.HashMap;
import java.util.Map;

public class Group {
    String value;
    Map<String, String> label = new HashMap<>();

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }
}
