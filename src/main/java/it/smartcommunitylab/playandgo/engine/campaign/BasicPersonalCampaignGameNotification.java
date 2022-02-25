package it.smartcommunitylab.playandgo.engine.campaign;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.mq.GamificationMessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ManageGameNotification;
import it.smartcommunitylab.playandgo.engine.notification.PersonalCampaignNotificationManager;

@Component
public class BasicPersonalCampaignGameNotification implements ManageGameNotification {
	private static Log logger = LogFactory.getLog(BasicPersonalCampaignGameNotification.class);
	
	@Autowired
	GamificationMessageQueueManager gamificationMessageQueueManager;
	
	@Autowired
	PersonalCampaignNotificationManager notificationManager;

	@PostConstruct
	public void init() {
		gamificationMessageQueueManager.setManageGameNotification(this, "111");
	}
	
 	@Override
	public void manageGameNotification(String msg, String routingKey) {
 		try {
			notificationManager.processNotification(msg);
		} catch (Exception e) {
			logger.error(String.format("manageGameNotification error:%s - %s", routingKey, e.getMessage()));
		}
	}
}
