package it.smartcommunitylab.playandgo.engine.model;

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
	private String nickname;
	@Indexed
	private String campaignId;
    @Indexed
    private String groupId;	
	@Indexed
	private Boolean global = Boolean.FALSE; 
	@Indexed
	private String modeType;
	@Indexed
	private String day;
	private String weekOfYear;
	private String monthOfYear;
	private double distance = 0.0; //meters
	private long duration = 0L; // seconds
	private double co2 = 0.0;
	private long trackNumber = 0L;
	
	public void addTrack() {
		this.trackNumber++;
	}
	
	public void subTrack() {
		this.trackNumber--;
	}
	
	public void addDistance(double distance) {
		this.distance += distance;
	}
	
	public void subDistance(double distance) {
		this.distance -= distance;
	}
	
	public void addDuration(long duration) {
		this.duration += duration;
	}
	
	public void subDuration(long duration) {
		this.duration -= duration;
	}
	
	public void addCo2(double co2) {
		this.co2 += co2;
	}
	
	public void subCo2(double co2) {
		this.co2 -= co2;
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

	public String getWeekOfYear() {
		return weekOfYear;
	}

	public void setWeekOfYear(String weekOfYear) {
		this.weekOfYear = weekOfYear;
	}

	public String getMonthOfYear() {
		return monthOfYear;
	}

	public void setMonthOfYear(String monthOfYear) {
		this.monthOfYear = monthOfYear;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
	
	
}
