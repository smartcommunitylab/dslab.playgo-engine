package it.smartcommunitylab.playandgo.engine.campaign;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.mq.GamificationMessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ManageGameNotification;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;

@Component
public class CityCampaignGameNotification implements ManageGameNotification {
	private static Logger logger = LoggerFactory.getLogger(CityCampaignGameNotification.class);
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	GamificationMessageQueueManager gamificationMessageQueueManager;

	@PostConstruct
	public void init() {
		gamificationMessageQueueManager.setManageGameNotification(this, Type.city);
		List<Campaign> list = campaignRepository.findByType(Type.city, Sort.by(Sort.Direction.DESC, "dateFrom"));
		list.forEach(c -> {
			gamificationMessageQueueManager.setGameNotification(c.getGameId());
			logger.debug(String.format("campaign %s subscribe to game %s", c.getCampaignId(), c.getGameId()));
		});
	}
	
	public void subcribeCampaing(Campaign c) {
		gamificationMessageQueueManager.setGameNotification(c.getGameId());
		logger.debug(String.format("campaign %s subscribe to game %s", c.getCampaignId(), c.getGameId()));		
	}
	
	@Override
	public void manageGameNotification(Map<String, Object> msg, String routingKey) {
		// TODO manage game notification
		
	}
	
}
