package it.smartcommunitylab.playandgo.engine.model.conf;

public class PointConf {
    String mean;
    String metric;
    double coefficient;

    public String getMean() {
        return mean;
    }

    public String getMetric() {
        return metric;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public void setMean(String mean) {
        this.mean = mean;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }
}
