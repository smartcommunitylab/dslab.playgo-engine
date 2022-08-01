package it.smartcommunitylab.playandgo.engine.campaign.city;

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
import it.smartcommunitylab.playandgo.engine.notification.CampaignNotificationManager;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class CityCampaignGameNotification implements ManageGameNotification {
	private static Logger logger = LoggerFactory.getLogger(CityCampaignGameNotification.class);
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	GamificationMessageQueueManager gamificationMessageQueueManager;
	
	@Autowired
	CampaignNotificationManager notificationManager;

	@Autowired
	CityCampaignGameStatusManager gameStatusManager;
	
	@Autowired
	CityCampaignChallengeNotification challengeNotification;

	@PostConstruct
	public void init() {
		gamificationMessageQueueManager.setManageGameNotification(this, Type.city);
		List<Campaign> list = campaignRepository.findByType(Type.city, Sort.by(Sort.Direction.DESC, "dateFrom"));
		list.forEach(c -> {
			if(Utils.isNotEmpty(c.getGameId())) {
				gamificationMessageQueueManager.setGameNotification(c.getGameId());
				logger.debug(String.format("campaign %s subscribe to game %s", c.getCampaignId(), c.getGameId()));					
			}
		});
	}
	
	public void subcribeCampaing(Campaign c) {
		if(Utils.isNotEmpty(c.getGameId())) {
			gamificationMessageQueueManager.setGameNotification(c.getGameId());
			logger.debug(String.format("campaign %s subscribe to game %s", c.getCampaignId(), c.getGameId()));					
		}
	}
	
	@Override
	public void manageGameNotification(Map<String, Object> msg, String routingKey) {
		String type = (String) msg.get("type");
		if(type.endsWith("GameNotification")) {
			gameStatusManager.updatePlayerGameStatus(msg);
		} else {
			if(type.equalsIgnoreCase("challenge_complete")) {
				challengeNotification.challengeCompleted(msg);
			}
			if(type.equalsIgnoreCase("challenge_failed")) {
				challengeNotification.challengeFailed(msg);
			}
			try {
				notificationManager.processNotification(msg);
			} catch (Exception e) {
				logger.error(String.format("manageGameNotification error:%s - %s", routingKey, e.getMessage()));
			}					
		}
	}
	
}
