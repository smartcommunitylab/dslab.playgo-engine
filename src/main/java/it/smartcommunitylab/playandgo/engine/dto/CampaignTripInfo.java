package it.smartcommunitylab.playandgo.engine.dto;

import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;

public class CampaignTripInfo {
	private String campaignId;
	private String campaignName;
	private Type type;
	private double score = 0.0;
	private ScoreStatus scoreStatus = ScoreStatus.UNASSIGNED;
	private boolean valid;
	
	public String getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	public String getCampaignName() {
		return campaignName;
	}
	public void setCampaignName(String campaignName) {
		this.campaignName = campaignName;
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
}
