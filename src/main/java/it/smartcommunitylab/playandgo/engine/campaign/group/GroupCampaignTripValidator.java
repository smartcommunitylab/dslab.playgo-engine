package it.smartcommunitylab.playandgo.engine.campaign.group;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.campaign.BasicCampaignTripValidator;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;

@Component
public class GroupCampaignTripValidator extends BasicCampaignTripValidator {
    
	@PostConstruct
	public void init() {
		groupIdKey = GroupCampaignSubscription.groupIdKey;
		queueManager.setManageValidateCampaignTripRequest(this, Type.group);
	}

}
