package it.smartcommunitylab.playandgo.engine.dto;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;

public class PlayerCampaignDTO {
	private Campaign campaign;
	private CampaignSubscription subscription;
	
	public PlayerCampaignDTO() {}
	
	public PlayerCampaignDTO(Campaign campaign, CampaignSubscription subscription) {
		this.campaign = campaign;
		this.subscription = subscription;
	}
	
	public Campaign getCampaign() {
		return campaign;
	}
	public void setCampaign(Campaign campaign) {
		this.campaign = campaign;
	}
	public CampaignSubscription getSubscription() {
		return subscription;
	}
	public void setSubscription(CampaignSubscription subscription) {
		this.subscription = subscription;
	}
}
