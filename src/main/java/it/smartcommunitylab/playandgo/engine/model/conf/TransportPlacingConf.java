package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportPlacingConf {
    Map<String, String> label = new HashMap<>();
    List<String> metrics = new ArrayList<>();
    String defaultMetric;
    List<String> periods = new ArrayList<>();
    String defaultPeriod;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
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
