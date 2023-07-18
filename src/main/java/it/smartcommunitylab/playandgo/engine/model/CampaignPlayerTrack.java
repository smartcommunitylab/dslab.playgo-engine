package it.smartcommunitylab.playandgo.engine.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="campaignPlayerTracks")
public class CampaignPlayerTrack {
	public enum ScoreStatus {
		UNASSIGNED, COMPUTED, SENT, ASSIGNED
	}
	
	@Id
	private String id;
	
	@Indexed
	private String playerId;
	@Indexed
	private String campaignId;
	private String campaignSubscriptionId;
	@Indexed
	private String trackedInstanceId;
	private String territoryId;
    private String groupId;    

	private boolean valid = false;
	private String errorCode;
	private ScoreStatus scoreStatus = ScoreStatus.UNASSIGNED;
	private double score;
	
	private Date startTime;
	private Date endTime;
	private String modeType;
	private double distance; //meters
	private long duration; //seconds
	private double co2;	//Kgr
	private double virtualScore;
	private boolean virtualTrack;
	
	private Map<String, Object> trackingData = new HashMap<>();
	
	
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
	public String getCampaignSubscriptionId() {
		return campaignSubscriptionId;
	}
	public void setCampaignSubscriptionId(String campaignSubscriptionId) {
		this.campaignSubscriptionId = campaignSubscriptionId;
	}
	public String getTrackedInstanceId() {
		return trackedInstanceId;
	}
	public void setTrackedInstanceId(String trackedInstanceId) {
		this.trackedInstanceId = trackedInstanceId;
	}
	public ScoreStatus getScoreStatus() {
		return scoreStatus;
	}
	public void setScoreStatus(ScoreStatus scoreStatus) {
		this.scoreStatus = scoreStatus;
	}
	public String getTerritoryId() {
		return territoryId;
	}
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}
	public Map<String, Object> getTrackingData() {
		return trackingData;
	}
	public void setTrackingData(Map<String, Object> trackingData) {
		this.trackingData = trackingData;
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
	public boolean isValid() {
		return valid;
	}
	public void setValid(boolean valid) {
		this.valid = valid;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
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
    public String getGroupId() {
        return groupId;
    }
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    public double getVirtualScore() {
        return virtualScore;
    }
    public void setVirtualScore(double virtualScore) {
        this.virtualScore = virtualScore;
    }
    public boolean isVirtualTrack() {
        return virtualTrack;
    }
    public void setVirtualTrack(boolean virtualTrack) {
        this.virtualTrack = virtualTrack;
    }

}
