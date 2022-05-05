package it.smartcommunitylab.playandgo.engine.dto;

import java.util.Map;

import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;

public class TrackedInstancePoly {
	private TrackedInstance trackedInstance;
	private String trackPolyline;
	private Map<String, Object> routesPolylines;
	
	public TrackedInstance getTrackedInstance() {
		return trackedInstance;
	}
	public void setTrackedInstance(TrackedInstance trackedInstance) {
		this.trackedInstance = trackedInstance;
	}
	public String getTrackPolyline() {
		return trackPolyline;
	}
	public void setTrackPolyline(String trackPolyline) {
		this.trackPolyline = trackPolyline;
	}
	public Map<String, Object> getRoutesPolylines() {
		return routesPolylines;
	}
	public void setRoutesPolylines(Map<String, Object> routesPolylines) {
		this.routesPolylines = routesPolylines;
	}
	
}
