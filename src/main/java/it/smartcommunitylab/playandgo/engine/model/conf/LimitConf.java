package it.smartcommunitylab.playandgo.engine.model.conf;

public class LimitConf {
    String mean;
    int dailyLimit;
    int weeklyLimit;
    int monthlyLimit;

    public String getMean() {
        return mean;
    }

    public void setMean(String mean) {
        this.mean = mean;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getWeeklyLimit() {
        return weeklyLimit;
    }

    public void setWeeklyLimit(int weeklyLimit) {
        this.weeklyLimit = weeklyLimit;
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(int monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }
}
