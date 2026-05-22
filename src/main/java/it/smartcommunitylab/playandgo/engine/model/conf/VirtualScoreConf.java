package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualScoreConf {
    Map<String, String> limitCompanyDesc = new HashMap<>();
    String label;
    double scoreDailyLimit = -1.0;
    double scoreWeeklyLimit = -1.0;
    double scoreMonthlyLimit = -1.0;
    int trackDailyLimit = -1;
    int trackWeeklyLimit = -1;
    int trackMonthlyLimit = -1;
    String firstLimitBar;
    String secondLimitBar;
    List<PointConf> points = new ArrayList<>();

    public Map<String, String> getLimitCompanyDesc() {
        return limitCompanyDesc;
    }

    public String getLabel() {
        return label;
    }

    public double getScoreDailyLimit() {
        return scoreDailyLimit;
    }

    public double getScoreWeeklyLimit() {
        return scoreWeeklyLimit;
    }

    public double getScoreMonthlyLimit() {
        return scoreMonthlyLimit;
    }

    public int getTrackDailyLimit() {
        return trackDailyLimit;
    }

    public int getTrackWeeklyLimit() {
        return trackWeeklyLimit;
    }

    public int getTrackMonthlyLimit() {
        return trackMonthlyLimit;
    }

    public String getFirstLimitBar() {
        return firstLimitBar;
    }

    public String getSecondLimitBar() {
        return secondLimitBar;
    }

    public List<PointConf> getPoints() {
        return points;
    }

    public void setLimitCompanyDesc(Map<String, String> limitCompanyDesc) {
        this.limitCompanyDesc = limitCompanyDesc;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setScoreDailyLimit(double scoreDailyLimit) {
        this.scoreDailyLimit = scoreDailyLimit;
    }

    public void setScoreWeeklyLimit(double scoreWeeklyLimit) {
        this.scoreWeeklyLimit = scoreWeeklyLimit;
    }

    public void setScoreMonthlyLimit(double scoreMonthlyLimit) {
        this.scoreMonthlyLimit = scoreMonthlyLimit;
    }

    public void setTrackDailyLimit(int trackDailyLimit) {
        this.trackDailyLimit = trackDailyLimit;
    }

    public void setTrackWeeklyLimit(int trackWeeklyLimit) {
        this.trackWeeklyLimit = trackWeeklyLimit;
    }

    public void setTrackMonthlyLimit(int trackMonthlyLimit) {
        this.trackMonthlyLimit = trackMonthlyLimit;
    }

    public void setFirstLimitBar(String firstLimitBar) {
        this.firstLimitBar = firstLimitBar;
    }

    public void setSecondLimitBar(String secondLimitBar) {
        this.secondLimitBar = secondLimitBar;
    }

    public void setPoints(List<PointConf> points) {
        this.points = points;
    }
}
