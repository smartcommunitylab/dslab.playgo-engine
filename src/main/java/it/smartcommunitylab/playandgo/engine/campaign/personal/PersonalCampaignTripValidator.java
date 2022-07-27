package it.smartcommunitylab.playandgo.engine.campaign.personal;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.campaign.BasicCampaignTripValidator;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;

@Component
public class PersonalCampaignTripValidator extends BasicCampaignTripValidator {
	private static Logger logger = LoggerFactory.getLogger(PersonalCampaignTripValidator.class);

	@PostConstruct
	public void init() {
		queueManager.setManageValidateCampaignTripRequest(this, Type.personal);
	}

}
