package it.smartcommunitylab.playandgo.engine.campaign;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.mq.GamificationMessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ManageGameNotification;
import it.smartcommunitylab.playandgo.engine.notification.PersonalCampaignNotificationManager;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;

@Component
public class BasicPersonalCampaignGameNotification implements ManageGameNotification {
	private static Log logger = LogFactory.getLog(BasicPersonalCampaignGameNotification.class);
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	GamificationMessageQueueManager gamificationMessageQueueManager;
	
	@Autowired
	PersonalCampaignNotificationManager notificationManager;

	@PostConstruct
	public void init() {
		List<Campaign> list = campaignRepository.findByType(Type.personal, Sort.by(Sort.Direction.DESC, "dateFrom"));
		list.forEach(c -> {
			gamificationMessageQueueManager.setManageGameNotification(this, c.getGameId());
		});
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
