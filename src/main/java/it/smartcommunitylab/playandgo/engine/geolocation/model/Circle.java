package it.smartcommunitylab.playandgo.engine.geolocation.model;

import it.smartcommunitylab.playandgo.engine.util.GamificationHelper;

public class Circle extends Shape {

	private double lat;
	private double lon;
	private double radius; // in meters

	public Circle() {
	}
	
	public Circle(double lat, double lon, double radius) {
		this.lat = lat;
		this.lon = lon;
		this.radius = radius;
	}

	public double getLat() {
		return lat;
	}	
	
	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	@Override
	public boolean inside(double lat, double lon) {
		// radius is in meters
		return GamificationHelper.harvesineDistance(getLat(), getLon(), lat, lon) <= (getRadius() / 1000.0);
	}

}