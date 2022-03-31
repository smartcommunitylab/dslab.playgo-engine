package it.smartcommunitylab.playandgo.engine.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult.TravelValidity;

public class TrackedInstanceInfo {
	private String trackedInstanceId;
	private String multimodalId;
	private Date startTime;
	private Date endTime;
	private String modeType;
	private double distance = 0.0; // meters
	private TravelValidity validity;
	private List<CampaignTripInfo> campaigns = new ArrayList<>();
	private Object polyline;
	
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
	public TravelValidity getValidity() {
		return validity;
	}
	public void setValidity(TravelValidity validity) {
		this.validity = validity;
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
	public String getTrackedInstanceId() {
		return trackedInstanceId;
	}
	public void setTrackedInstanceId(String trackedInstanceId) {
		this.trackedInstanceId = trackedInstanceId;
	}
	public String getMultimodalId() {
		return multimodalId;
	}
	public void setMultimodalId(String multimodalId) {
		this.multimodalId = multimodalId;
	}
	public String getModeType() {
		return modeType;
	}
	public void setModeType(String modeType) {
		this.modeType = modeType;
	}
	public Object getPolyline() {
		return polyline;
	}
	public void setPolyline(Object polyline) {
		this.polyline = polyline;
	}
	
}
