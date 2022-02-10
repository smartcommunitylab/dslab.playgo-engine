package it.smartcommunitylab.playandgo.engine.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="campaignSubscriptions")
public class CampaignSubscription {
	@Id
	private String id;
	@Indexed
	private String playerId;
	@Indexed
	private String campaignId;
	private String mail;
	private Boolean sendMail = Boolean.FALSE;
	private Map<String, Object> campaignData;
	
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
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}
	public Map<String, Object> getCampaignData() {
		return campaignData;
	}
	public void setCampaignData(Map<String, Object> campaignData) {
		this.campaignData = campaignData;
	}
	public Boolean getSendMail() {
		return sendMail;
	}
	public void setSendMail(Boolean sendMail) {
		this.sendMail = sendMail;
	}
}
