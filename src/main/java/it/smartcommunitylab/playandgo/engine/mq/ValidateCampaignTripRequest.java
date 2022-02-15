package it.smartcommunitylab.playandgo.engine.mq;

public class ValidateCampaignTripRequest {
	private String playerId;
	private String campaignId;
	private String campaignSubscriptionId;
	private String trackedInstanceId;
	private String campaignPlayerTrackId;
	private String territoryId;
	
	public ValidateCampaignTripRequest() {}
	
	public ValidateCampaignTripRequest(String playerId, String territoryId, String trackedInstanceId, 
			String campaignId, String campaignSubscriptionId, String campaignPlayerTrackId) {
		this.playerId = playerId;
		this.territoryId = territoryId;
		this.trackedInstanceId = trackedInstanceId;
		this.campaignId = campaignId;
		this.campaignPlayerTrackId = campaignPlayerTrackId;
		this.campaignSubscriptionId = campaignSubscriptionId;
	}
	
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public String getTrackedInstanceId() {
		return trackedInstanceId;
	}
	public void setTrackedInstanceId(String trackedInstanceId) {
		this.trackedInstanceId = trackedInstanceId;
	}
	public String getTerritoryId() {
		return territoryId;
	}
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
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

	public String getCampaignPlayerTrackId() {
		return campaignPlayerTrackId;
	}

	public void setCampaignPlayerTrackId(String campaignPlayerTrackId) {
		this.campaignPlayerTrackId = campaignPlayerTrackId;
	}
}	
