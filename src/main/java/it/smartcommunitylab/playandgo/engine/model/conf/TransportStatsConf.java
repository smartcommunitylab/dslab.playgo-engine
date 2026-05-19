package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportStatsConf {
    Map<String, String> label = new HashMap<>();
    String defaultMean;
    List<String> metrics = new ArrayList<>();
    String defaultMetric;
    List<String> groupMode = new ArrayList<>();
    String defaultGroupMode;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getDefaultMean() {
        return defaultMean;
    }

    public void setDefaultMean(String defaultMean) {
        this.defaultMean = defaultMean;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public String getDefaultMetric() {
        return defaultMetric;
    }

    public void setDefaultMetric(String defaultMetric) {
        this.defaultMetric = defaultMetric;
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
