package it.smartcommunitylab.playandgo.engine.ge;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.notification.CampaignNotificationManager;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@EnableScheduling
public class ChallengeNotificationTask {
    private static final Logger logger = LoggerFactory.getLogger(ChallengeNotificationTask.class);
    
    @Autowired
    GamificationEngineManager gamificationEngineManager;
    
    @Autowired
    CampaignNotificationManager notificationManager;
    
    @Autowired
    CampaignRepository campaignRepository;
    
    @Autowired
    TerritoryRepository territoryRepository;
    
    @Scheduled(cron="${gamification.cron}")
    @SchedulerLock(name = "ChallengeNotificationTask.sendWeeklyNotification")
    public void sendWeeklyNotificationProposed() {
        sendNotifications(Campaign.challengePlayerProposed, "PROPOSED");
        sendNotifications(Campaign.challengePlayerAssigned, "ASSIGNED");
    }
    
    private ZonedDateTime getZonedDateTime(Campaign campaign) {
        ZoneId zoneId = null;
        Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
        if(territory == null) {
            zoneId = ZoneId.systemDefault();
        } else {
            zoneId = ZoneId.of(territory.getTimezone());
        }
        return ZonedDateTime.now(zoneId);        
    }
    
    private void sendNotifications(String cronKey, String messageKey) {
        List<Campaign> campaigns = campaignRepository.findAll();
        for(Campaign campaign : campaigns) {
            if(campaign.currentlyActive() && Utils.isNotEmpty(campaign.getGameId()) 
                    && Utils.isNotEmpty((String)campaign.getSpecificData().get(cronKey))) {
                CronExpression expression = CronExpression.parse((String)campaign.getSpecificData().get(cronKey));
                ZonedDateTime nowZoned = getZonedDateTime(campaign);
                ZonedDateTime truncatedTime = nowZoned.truncatedTo(ChronoUnit.HOURS);
                ZonedDateTime nextExpression = expression.next(truncatedTime.minusMinutes(1));
                if(nextExpression.equals(truncatedTime)) {
                    logger.info(String.format("sendNotification: %s - %s - %s", campaign.getCampaignId(), campaign.getGameId(), messageKey));
                    List<String> playerList = gamificationEngineManager.getProposedPlayerList(campaign.getGameId());
                    for(String playerId : playerList) {
                        try {
                            Map<String, Object> msg = new HashMap<>();
                            Map<String, Object> obj = new HashMap<>();
                            obj.put("gameId", campaign.getGameId());
                            obj.put("playerId", playerId);
                            obj.put("timestamp", System.currentTimeMillis());
                            obj.put("key", messageKey);
                            msg.put("type", "MessageNotification");
                            msg.put("obj", obj);
                            notificationManager.processNotification(msg);
                        } catch (Exception e) {
                            logger.error(String.format("sendNotification error:%s - %s", playerId, e.getMessage()));
                        }                                               
                    }
                }
            }
        }        
    }
    

        
}
