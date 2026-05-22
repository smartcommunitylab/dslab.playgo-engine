package it.smartcommunitylab.playandgo.engine.model.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualScoreConf {
    Map<String, String> limitCompanyDesc = new HashMap<>();
    List<LimitConf> limits = new ArrayList<>();
    String label;
    int scoreDailyLimit = -1;
    int scoreWeeklyLimit = -1;
    int scoreMonthlyLimit = -1;
    int trackDailyLimit = -1;
    int trackWeeklyLimit = -1;
    int trackMonthlyLimit = -1;
    String firstLimitBar;
    String secondLimitBar;
    List<PointConf> points = new ArrayList<>();

    public Map<String, String> getLimitCompanyDesc() {
        return limitCompanyDesc;
    }

    public List<LimitConf> getLimits() {
        return limits;
    }

    public String getLabel() {
        return label;
    }

    public int getScoreDailyLimit() {
        return scoreDailyLimit;
    }

    public int getScoreWeeklyLimit() {
        return scoreWeeklyLimit;
    }

    public int getScoreMonthlyLimit() {
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

    public void setLimits(List<LimitConf> limits) {
        this.limits = limits;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setScoreDailyLimit(int scoreDailyLimit) {
        this.scoreDailyLimit = scoreDailyLimit;
    }

    public void setScoreWeeklyLimit(int scoreWeeklyLimit) {
        this.scoreWeeklyLimit = scoreWeeklyLimit;
    }

    public void setScoreMonthlyLimit(int scoreMonthlyLimit) {
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
