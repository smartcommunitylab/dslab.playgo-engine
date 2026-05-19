package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamePlacingConf {
    Map<String, String> label = new HashMap<>();
    List<String> periods = new ArrayList<>();
    String defaultPeriod;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public List<String> getPeriods() {
        return periods;
    }

    public void setPeriods(List<String> periods) {
        this.periods = periods;
    }

    public String getDefaultPeriod() {
        return defaultPeriod;
    }

    public void setDefaultPeriod(String defaultPeriod) {
        this.defaultPeriod = defaultPeriod;
    }
}
