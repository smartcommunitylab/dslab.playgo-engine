package it.smartcommunitylab.playandgo.engine.manager.azienda;

public class LegResult {
	private double distance;
	private String id;
	private String mean;
	private double validDistance;
	private double virtualScore;
	
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getMean() {
		return mean;
	}
	public void setMean(String mean) {
		this.mean = mean;
	}
	public double getValidDistance() {
		return validDistance;
	}
	public void setValidDistance(double validDistance) {
		this.validDistance = validDistance;
	}
    public double getVirtualScore() {
        return virtualScore;
    }
    public void setVirtualScore(double virtualScore) {
        this.virtualScore = virtualScore;
    }
}
