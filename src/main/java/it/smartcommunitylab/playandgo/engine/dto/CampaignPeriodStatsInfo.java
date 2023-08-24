package it.smartcommunitylab.playandgo.engine.dto;

public class CampaignPeriodStatsInfo {
    private long dateFrom;
    private long dateTo;
    private String dateFromS;
    private String dateToS;
    private double value = 0.0;
    
    public void addValue(double value) {
        this.value += value;
    }
    
    public double getValue() {
        return value;
    }
    public void setValue(double value) {
        this.value = value;
    }
    public long getDateFrom() {
        return dateFrom;
    }
    public void setDateFrom(long dateFrom) {
        this.dateFrom = dateFrom;
    }
    public long getDateTo() {
        return dateTo;
    }
    public void setDateTo(long dateTo) {
        this.dateTo = dateTo;
    }
    public String getDateFromS() {
        return dateFromS;
    }
    public void setDateFromS(String dateFromS) {
        this.dateFromS = dateFromS;
    }
    public String getDateToS() {
        return dateToS;
    }
    public void setDateToS(String dateToS) {
        this.dateToS = dateToS;
    }
}
