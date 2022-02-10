package it.smartcommunitylab.playandgo.engine.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="campaignPlayerSurveys")
public class CampaignPlayerSurvey {
	@Id
	private String id;
	
	private String playerId;
	private String campaignId;
	private String campaignSubscriptionId;
	
	private Map<String,Map<String,Object>> surveys;

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

	public Map<String, Map<String, Object>> getSurveys() {
		return surveys;
	}

	public void setSurveys(Map<String, Map<String, Object>> surveys) {
		this.surveys = surveys;
	}
}
