package it.smartcommunitylab.playandgo.engine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Circle;

public class ValidationData {
    private List<String> means = new ArrayList<>();
    private Circle area;
    private Map<String, Object> validationMap;

    public List<String> getMeans() {
        return means;
    }
    public void setMeans(List<String> means) {
        this.means = means;
    }
    public Circle getArea() {
        return area;
    }
    public void setArea(Circle area) {
        this.area = area;
    }
    public Map<String, Object> getValidationMap() {
        return validationMap;
    }
    public void setValidationMap(Map<String, Object> validationMap) {
        this.validationMap = validationMap;
    }

}
