package it.smartcommunitylab.playandgo.engine.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="playerStatsTransports")
public class PlayerStatsTransport {
	@Id
	private String id;
	@Indexed
	private String playerId;
	@Indexed
	private String campaignId;
	@Indexed
	private Boolean global = Boolean.FALSE; 
	@Indexed
	private String modeType;
	@Indexed
	private LocalDate weeklyDay;
	private double distance = 0.0; //meters
	private long duration = 0L; // seconds
	private double co2 = 0.0;
	private long trackNumber = 0L;
	
	public void addTrack() {
		this.trackNumber++;
	}
	
	public void addDistance(double distance) {
		this.distance += distance;
	}
	
	public void addDuration(long duration) {
		this.duration += duration;
	}
	
	public void addCo2(double co2) {
		this.co2 += co2;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public String getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	public Boolean getGlobal() {
		return global;
	}
	public void setGlobal(Boolean global) {
		this.global = global;
	}
	public LocalDate getWeeklyDay() {
		return weeklyDay;
	}
	public void setWeeklyDay(LocalDate weeklyDay) {
		this.weeklyDay = weeklyDay;
	}
	public String getModeType() {
		return modeType;
	}
	public void setModeType(String modeType) {
		this.modeType = modeType;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
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
	public long getTrackNumber() {
		return trackNumber;
	}
	public void setTrackNumber(long trackNumber) {
		this.trackNumber = trackNumber;
	}
	
	
}
