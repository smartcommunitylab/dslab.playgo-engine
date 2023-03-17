package it.smartcommunitylab.playandgo.engine.ge;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.notification.CampaignNotificationManager;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@EnableScheduling
public class ChallengeNotificationTask {
    private static final Logger logger = LoggerFactory.getLogger(ChallengeNotificationTask.class);
    
    @Value("${gamification.cronProposed}")
    private String cronProposed;
    
    @Value("${gamification.cronAssigned}")
    private String cronAssigned;
    
    @Autowired
    GamificationEngineManager gamificationEngineManager;
    
    @Autowired
    CampaignNotificationManager notificationManager;
    
    @Autowired
    CampaignRepository campaignRepository;

    @Scheduled(cron="${this.cronProposed}")
    @SchedulerLock(name = "ChallengeNotificationTask.sendWeeklyNotificationProposed")
    public void sendWeeklyNotificationProposed() {
        List<Campaign> campaigns = campaignRepository.findAll();
        for(Campaign campaign : campaigns) {
            if(campaign.currentlyActive() && Utils.isNotEmpty(campaign.getGameId()) 
                    && Utils.isNotEmpty((String)campaign.getSpecificData().get(Campaign.challengePlayerProposed))) {
                CronExpression expression = CronExpression.parse((String)campaign.getSpecificData().get(Campaign.challengePlayerProposed));
                LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);;
                LocalDateTime next = expression.next(now);
                if(next.equals(now)) {
                    logger.info(String.format("sendWeeklyNotificationProposed: %s - %s", campaign.getCampaignId(), campaign.getGameId()));
                    List<String> playerList = gamificationEngineManager.getProposedPlayerList(campaign.getGameId());
                    for(String playerId : playerList) {
                        
                    }
                }
            }
        }
    }
    
    @Scheduled(cron="${this.cronAssigned}")
    @SchedulerLock(name = "ChallengeNotificationTask.sendWeeklyNotificationAssigned")
    public void sendWeeklyNotificationAssigned() {
        
    }
    

        
}
