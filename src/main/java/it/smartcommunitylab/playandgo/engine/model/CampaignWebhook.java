package it.smartcommunitylab.playandgo.engine.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="campaignWebhooks")
public class CampaignWebhook {
	public static enum EventType {
		register, unregister, validTrack
	}
	
	@Id
	private String id;
	@Indexed
	private String campaignId;
	private List<EventType> events = new ArrayList<>();
	private String endpoint;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	public List<EventType> getEvents() {
		return events;
	}
	public void setEvents(List<EventType> events) {
		this.events = events;
	}
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	
	
}
