package it.smartcommunitylab.playandgo.engine.mq;

public class ValidateCampaignTripRequest {
	private String playerId;
	private String campaignId;
	private String campaignSubscriptionId;
	private String multimodalId;
	private String territoryId;
	private String campaignType;
	
	public ValidateCampaignTripRequest() {}
	
	public ValidateCampaignTripRequest(String playerId, String territoryId, String multimodalId, 
			String campaignId, String campaignSubscriptionId, String campaignType) {
		this.playerId = playerId;
		this.territoryId = territoryId;
		this.multimodalId = multimodalId;
		this.campaignId = campaignId;
		this.campaignSubscriptionId = campaignSubscriptionId;
		this.campaignType = campaignType;
	}
	
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
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

	public String getCampaignType() {
		return campaignType;
	}

	public void setCampaignType(String campaignType) {
		this.campaignType = campaignType;
	}

    public String getMultimodalId() {
        return multimodalId;
    }

    public void setMultimodalId(String multimodalId) {
        this.multimodalId = multimodalId;
    }
}	
