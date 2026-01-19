package it.smartcommunitylab.playandgo.engine.campaign.school;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.campaign.BasicCampaignTripValidator;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;

@Component
public class SchoolCampaignTripValidator extends BasicCampaignTripValidator {
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(SchoolCampaignTripValidator.class);
	
	@Autowired
	MessageQueueManager queueManager;

	@PostConstruct
	public void init() {
	    groupIdKey = SchoolCampaignSubscription.groupIdKey;
		queueManager.setManageValidateCampaignTripRequest(this, Type.school);
	}
	
}
