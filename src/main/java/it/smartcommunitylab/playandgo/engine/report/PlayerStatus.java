package it.smartcommunitylab.playandgo.engine.report;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.smartcommunitylab.playandgo.engine.model.Territory;

public class PlayerStatus {
	private String playerId;
	private Date registrationDate;
	private int activityDays;
	private long travels;
	private List<TransportStats> transportStatsList = new ArrayList<>();
	private Territory territory;
	private double co2;
	
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public int getActivityDays() {
		return activityDays;
	}
	public void setActivityDays(int activityDays) {
		this.activityDays = activityDays;
	}
	public long getTravels() {
		return travels;
	}
	public void setTravels(long travels) {
		this.travels = travels;
	}
	public List<TransportStats> getTransportStatsList() {
		return transportStatsList;
	}
	public void setTransportStatsList(List<TransportStats> transportStatsList) {
		this.transportStatsList = transportStatsList;
	}
	public Territory getTerritory() {
		return territory;
	}
	public void setTerritory(Territory territory) {
		this.territory = territory;
	}
	public double getCo2() {
		return co2;
	}
	public void setCo2(double co2) {
		this.co2 = co2;
	}
	public Date getRegistrationDate() {
		return registrationDate;
	}
	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}
}
