package it.smartcommunitylab.playandgo.engine.mq;

public interface ManageValidateCampaignTripRequest {
	public void validateTripRequest(ValidateCampaignTripRequest message);
	public void invalidateTripRequest(ValidateCampaignTripRequest message);
	public void updateTripRequest(UpdateCampaignTripRequest message);
}
