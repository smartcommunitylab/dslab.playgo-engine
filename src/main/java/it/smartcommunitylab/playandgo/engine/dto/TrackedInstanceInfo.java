package it.smartcommunitylab.playandgo.engine.dto;

import java.util.Date;

import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult.TravelValidity;

public class TrackedInstanceInfo {
	private Date startTime;
	private Date endTime;
	private TravelValidity validity;
	
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
	
}
