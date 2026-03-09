package it.smartcommunitylab.playandgo.engine.manager.survey;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.SurveyTaskRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@EnableScheduling
public class SurveyScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SurveyScheduler.class);

    @Autowired
    SurveyTaskRepository surveyTaskRepository; 

    @Autowired
    CampaignRepository campaignRepository;

	@Autowired
	SurveyManager surveyManager;
    
	@Scheduled(cron="0 0 3 * * *") // every day at 3am
	@SchedulerLock(name = "SurveyScheduler.assignSurvey")
    public void assignSurvey() {
        surveyTaskRepository.findAll().forEach(task -> {
            try {
                Campaign campaign = campaignRepository.findById(task.getCampaignId()).orElse(null);
                if(campaign == null || !campaign.currentlyActive()) {
                    logger.info(String.format("assignSurvey: skipping inactive campaign %s for task %s", 
                        task.getCampaignId(), task.getId()));
                    return;
                }
                surveyManager.assignSurveyChallenges(task.getCampaignId(), Arrays.asList(task.getPlayerId()), task.getSr());
                logger.info(String.format("assignSurvey: campaign %s - player %s - survey %s", 
					task.getCampaignId(), task.getPlayerId(), task.getSr().getSurveyName()));
                surveyTaskRepository.deleteById(task.getId());
            } catch (Exception e) {
                logger.error(String.format("Error assigning survey for task %s : %s", task.getId(), e.getMessage()));
            }
        });
    }
}
