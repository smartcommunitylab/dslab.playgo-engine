package it.smartcommunitylab.playandgo.engine.report;

public class TransportStat {
	private String period;
	private String mean;
	private double value = 0.0;
	
	public String getPeriod() {
		return period;
	}
	public void setPeriod(String period) {
		this.period = period;
	}
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
	}
	public String getMean() {
		return mean;
	}
	public void setMean(String mean) {
		this.mean = mean;
	}
}
