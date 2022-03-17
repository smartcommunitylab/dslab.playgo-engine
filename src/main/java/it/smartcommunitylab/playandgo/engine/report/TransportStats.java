package it.smartcommunitylab.playandgo.engine.report;

import org.springframework.data.annotation.Id;

public class TransportStats {
	@Id
	private String modeType;
	private double totalDistance;
	private long totalDuration;
	
	public String getModeType() {
		return modeType;
	}
	public void setModeType(String modeType) {
		this.modeType = modeType;
	}
	public double getTotalDistance() {
		return totalDistance;
	}
	public void setTotalDistance(double totalDistance) {
		this.totalDistance = totalDistance;
	}
	public long getTotalDuration() {
		return totalDuration;
	}
	public void setTotalDuration(long totalDuration) {
		this.totalDuration = totalDuration;
	}
}
