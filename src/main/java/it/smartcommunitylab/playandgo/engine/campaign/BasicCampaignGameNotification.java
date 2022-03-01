package it.smartcommunitylab.playandgo.engine.campaign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.mq.GamificationMessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ManageGameNotification;
import it.smartcommunitylab.playandgo.engine.notification.PersonalCampaignNotificationManager;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;

@Component
public class BasicCampaignGameNotification implements ManageGameNotification {
	private static Logger logger = LoggerFactory.getLogger(BasicCampaignGameNotification.class);
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	GamificationMessageQueueManager gamificationMessageQueueManager;
	
	@Autowired
	PersonalCampaignNotificationManager notificationManager;

 	@Override
	public void manageGameNotification(String msg, String routingKey) {
 		try {
			notificationManager.processNotification(msg);
		} catch (Exception e) {
			logger.error(String.format("manageGameNotification error:%s - %s", routingKey, e.getMessage()));
		}
	}
}
