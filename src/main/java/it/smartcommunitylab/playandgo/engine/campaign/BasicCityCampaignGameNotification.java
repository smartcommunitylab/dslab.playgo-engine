package it.smartcommunitylab.playandgo.engine.campaign;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;

@Component
public class BasicCityCampaignGameNotification extends BasicCampaignGameNotification {
	private static Logger logger = LoggerFactory.getLogger(BasicCityCampaignGameNotification.class);
	
	@PostConstruct
	public void init() {
		LocalDate today = LocalDate.now();
		List<Campaign> list = campaignRepository.findByType(Type.city, Sort.by(Sort.Direction.DESC, "dateFrom"));
		list.forEach(c -> {
			if((!today.isBefore(c.getDateFrom())) && (!today.isAfter(c.getDateTo()))) {
				gamificationMessageQueueManager.setManageGameNotification(this, c.getGameId());
				logger.debug(String.format("campaign %s subscribe to game %s", c.getCampaignId(), c.getGameId()));
			}		
		});
	}
	
}
