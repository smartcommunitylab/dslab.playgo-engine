package it.smartcommunitylab.playandgo.engine.campaign;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;

@Component
public class BasicPersonalCampaignTripValidator extends BasicCampaignTripValidator {
	private static Logger logger = LoggerFactory.getLogger(BasicPersonalCampaignTripValidator.class);

	@PostConstruct
	public void init() {
		List<Campaign> list = campaignRepository.findByType(Type.personal, Sort.by(Sort.Direction.DESC, "dateFrom"));
		list.forEach(c -> {
			queueManager.setManageValidateCampaignTripRequest(this, c.getTerritoryId(), c.getCampaignId());
			logger.debug(String.format("campaign %s subscribe to game %s", c.getCampaignId(), c.getGameId()));
		});
	}

	public void subcribeCampaing(Campaign c) {
		queueManager.setManageValidateCampaignTripRequest(this, c.getTerritoryId(), c.getCampaignId());
		logger.debug(String.format("campaign %s subscribe to game %s", c.getCampaignId(), c.getGameId()));
	}
	
	public void unsubcribeCampaing(Campaign c) {
		queueManager.unsetManageValidateCampaignTripRequest(c.getTerritoryId(), c.getCampaignId());
		logger.debug(String.format("campaign %s unsubscribe to game %s", c.getCampaignId(), c.getGameId()));
	}

}
