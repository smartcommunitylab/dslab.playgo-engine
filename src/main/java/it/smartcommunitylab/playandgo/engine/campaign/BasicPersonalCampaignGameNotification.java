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
public class BasicPersonalCampaignGameNotification extends BasicCampaignGameNotification {
	private static Logger logger = LoggerFactory.getLogger(BasicPersonalCampaignGameNotification.class);
	
	@PostConstruct
	public void init() {
		List<Campaign> list = campaignRepository.findByType(Type.personal, Sort.by(Sort.Direction.DESC, "dateFrom"));
		list.forEach(c -> {
			gamificationMessageQueueManager.setManageGameNotification(this, c.getGameId());
			logger.debug(String.format("campaign %s subscribe to game %s", c.getCampaignId(), c.getGameId()));
		});
	}
	
}
