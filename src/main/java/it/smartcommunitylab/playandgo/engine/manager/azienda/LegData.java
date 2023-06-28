package it.smartcommunitylab.playandgo.engine.manager.azienda;

import java.util.ArrayList;
import java.util.List;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;

public class LegData {
    private String id;
	private double distance;
	private String mean;
	private long duration;
	private double co2;
	private List<Geolocation> points = new ArrayList<>();
	
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
	public List<Geolocation> getPoints() {
		return points;
	}
	public void setPoints(List<Geolocation> points) {
		this.points = points;
	}
    public long getDuration() {
        return duration;
    }
    public void setDuration(long duration) {
        this.duration = duration;
    }
    public double getCo2() {
        return co2;
    }
    public void setCo2(double co2) {
        this.co2 = co2;
    }
}
