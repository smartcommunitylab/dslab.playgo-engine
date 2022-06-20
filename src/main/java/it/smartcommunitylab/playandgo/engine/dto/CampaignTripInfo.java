package it.smartcommunitylab.playandgo.engine.dto;

import java.util.HashMap;
import java.util.Map;

import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;

public class CampaignTripInfo {
	private String campaignId;
	private Map<String, String> campaignName = new HashMap<>();
	private Type type;
	private double score = 0.0;
	private double distance = 0.0;
	private ScoreStatus scoreStatus = ScoreStatus.UNASSIGNED;
	private boolean valid;
	private String errorCode;
	
	public String getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public boolean isValid() {
		return valid;
	}
	public void setValid(boolean valid) {
		this.valid = valid;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public ScoreStatus getScoreStatus() {
		return scoreStatus;
	}
	public void setScoreStatus(ScoreStatus scoreStatus) {
		this.scoreStatus = scoreStatus;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public Map<String, String> getCampaignName() {
		return campaignName;
	}
	public void setCampaignName(Map<String, String> campaignName) {
		this.campaignName = campaignName;
	}
}
