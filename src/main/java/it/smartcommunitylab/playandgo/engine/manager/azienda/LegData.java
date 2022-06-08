package it.smartcommunitylab.playandgo.engine.manager.azienda;

import java.util.ArrayList;
import java.util.List;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;

public class LegData {
	private double distance;
	private String id;
	private String mean;
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
}
