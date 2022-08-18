package it.smartcommunitylab.playandgo.engine.mq;

import java.util.HashMap;
import java.util.Map;

import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook.EventType;

public class WebhookRequest {
	private String campaignId;
	private String playerId;
	private EventType eventType;
	private Map<String, Object> data = new HashMap<>();
	
	public String getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public EventType getEventType() {
		return eventType;
	}
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	public Map<String, Object> getData() {
		return data;
	}
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	
	
}
