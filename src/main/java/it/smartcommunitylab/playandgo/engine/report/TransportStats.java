package it.smartcommunitylab.playandgo.engine.report;

import org.springframework.data.annotation.Id;

public class TransportStats {
	@Id
	private String modeType;
	private double totalDistance;
	private long totalDuration;
	private double totalCo2;
	private long totalTravel;
	
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
	public double getTotalCo2() {
		return totalCo2;
	}
	public void setTotalCo2(double totalCo2) {
		this.totalCo2 = totalCo2;
	}
	public long getTotalTravel() {
		return totalTravel;
	}
	public void setTotalTravel(long totalTravel) {
		this.totalTravel = totalTravel;
	}
}
