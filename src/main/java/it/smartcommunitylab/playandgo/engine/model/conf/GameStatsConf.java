package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameStatsConf {
    Map<String, String> label = new HashMap<>();
    List<String> groupMode = new ArrayList<>();
    String defaultGroupMode;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public List<String> getGroupMode() {
        return groupMode;
    }

    public void setGroupMode(List<String> groupMode) {
        this.groupMode = groupMode;
    }

    public String getDefaultGroupMode() {
        return defaultGroupMode;
    }

    public void setDefaultGroupMode(String defaultGroupMode) {
        this.defaultGroupMode = defaultGroupMode;
    }
}
