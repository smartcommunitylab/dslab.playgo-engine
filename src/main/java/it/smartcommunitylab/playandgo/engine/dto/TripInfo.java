package it.smartcommunitylab.playandgo.engine.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TripInfo {
	private String multimodalId;
	private Date startTime;
	private Date endTime;
	private double distance = 0.0; // meters
	private List<TrackedInstanceInfo> tracks = new ArrayList<>();
	private List<CampaignTripInfo> campaigns = new ArrayList<>();
	
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public List<CampaignTripInfo> getCampaigns() {
		return campaigns;
	}
	public void setCampaigns(List<CampaignTripInfo> campaigns) {
		this.campaigns = campaigns;
	}
	public String getMultimodalId() {
		return multimodalId;
	}
	public void setMultimodalId(String multimodalId) {
		this.multimodalId = multimodalId;
	}
	public List<TrackedInstanceInfo> getTracks() {
		return tracks;
	}
	public void setTracks(List<TrackedInstanceInfo> tracks) {
		this.tracks = tracks;
	}

}
