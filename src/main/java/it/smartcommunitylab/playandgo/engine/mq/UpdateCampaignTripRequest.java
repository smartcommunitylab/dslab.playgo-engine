package it.smartcommunitylab.playandgo.engine.mq;

public class UpdateCampaignTripRequest {
	private String campaignType;
	private String campaignPlayerTrackId;
	private double deltaDistance;
	
	
	public UpdateCampaignTripRequest() {}
	
	public UpdateCampaignTripRequest(String campaignType, String campaignPlayerTrackId, double deltaDistance) {
		this.campaignType = campaignType;
		this.campaignPlayerTrackId = campaignPlayerTrackId;
		this.deltaDistance = deltaDistance;
	}
	
	public String getCampaignPlayerTrackId() {
		return campaignPlayerTrackId;
	}

	public void setCampaignPlayerTrackId(String campaignPlayerTrackId) {
		this.campaignPlayerTrackId = campaignPlayerTrackId;
	}

	public double getDeltaDistance() {
		return deltaDistance;
	}

	public void setDeltaDistance(double deltaDistance) {
		this.deltaDistance = deltaDistance;
	}

	public String getCampaignType() {
		return campaignType;
	}

	public void setCampaignType(String campaignType) {
		this.campaignType = campaignType;
	}

}	
