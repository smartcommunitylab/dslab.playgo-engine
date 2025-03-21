package it.smartcommunitylab.playandgo.engine.mq;

public interface ManageValidateCampaignTripRequest {
	public void validateTripRequest(ValidateCampaignTripRequest message);
	public void invalidateTripRequest(UpdateCampaignTripRequest message);
	public void updateTripRequest(UpdateCampaignTripRequest message);
	public void revalidateTripRequest(UpdateCampaignTripRequest message);
}
