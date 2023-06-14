package it.smartcommunitylab.playandgo.engine.campaign.company;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerIdentity;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyInfo;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyRequest;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerSurvey;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.notification.EmailService;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerSurveyRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.EncryptDecrypt;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class CompanyCampaignSurveyManager {
	private static transient final Logger logger = LoggerFactory.getLogger(CompanyCampaignSurveyManager.class);

    @Autowired
    GamificationEngineManager gamificationManager;
    
	@Autowired
	CampaignRepository campaignRepository;

	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	CampaignPlayerSurveyRepository surveyRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	EmailService emailService;
	
	EncryptDecrypt cryptUtils;

	public void assignSurvey(String campaignId, List<String> playerIds, SurveyRequest sr) throws Exception {
		if(playerIds == null || playerIds.size() == 0) {
			playerIds = new ArrayList<>();
			List<CampaignSubscription> campaignSubscriptions = campaignSubscriptionRepository.findByCampaignId(campaignId);
			for(CampaignSubscription cs : campaignSubscriptions) {
				playerIds.add(cs.getPlayerId());
			}
		}
		for(String playerId : playerIds) {		 
		    CampaignPlayerSurvey survey = surveyRepository.findByPlayerIdAndCampaignIdAndSurveyName(playerId, campaignId, sr.getSurveyName());
		    if(survey == null) {
		        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		        Player player = playerRepository.findById(playerId).orElse(null);
		        if((campaign != null) && (player != null)) {
		            try {
	                    String surveyUrl = gamificationManager.createSurveyUrl(playerId, campaignId, sr.getSurveyName(), player.getLanguage());
	                    emailService.sendSurveyInvite(surveyUrl, campaign.getName().get(player.getLanguage()), player.getMail(), player.getLanguage());
	                    survey = new CampaignPlayerSurvey();
	                    survey.setPlayerId(playerId);
	                    survey.setCampaignId(campaignId);
	                    survey.setTimestamp(Utils.getUTCDate(System.currentTimeMillis()));
	                    survey.setSurveyLink(sr.getSurveyLink());
	                    survey.setSurveyName(sr.getSurveyName());
	                    surveyRepository.save(survey);                                          
                    } catch (Exception e) {
                        logger.warn(String.format("assignSurvey error:%s - %s - %s", playerId, sr.getSurveyName(), e.getMessage()));
                    }
		        }
		    }
		}
	}
	
	public boolean compileSurvey(String surveyName, Map<String,Object> formData) {
		boolean complete = false;
		try {
			String id = (String)formData.get("playerId");
			PlayerIdentity identity = gamificationManager.decryptIdentity(id);
			String playerId = identity.getPlayerId();
			String campaignId = identity.getGameId();
			if(Utils.isNotEmpty(playerId) && Utils.isNotEmpty(campaignId)) {
				Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
				if(campaign == null) {
					return false;
				}
				CampaignPlayerSurvey survey = surveyRepository.findByPlayerIdAndCampaignIdAndSurveyName(playerId, campaignId, surveyName);
				if(survey == null) {
					return false;
				}
                if(campaign.currentlyActive()) {
                    complete = true;
                    survey.setCompleted(true);
                    surveyRepository.save(survey);
                }
			}			
		} catch (Exception e) {
			logger.warn(String.format("compileSurvey error:%s - %s - %s", surveyName, formData, e.getMessage()));
		}
		return complete;
	}
	
	public SurveyInfo getSurveyUrl(String id, String surveyName) {
		SurveyInfo info = new SurveyInfo();
		try {
			PlayerIdentity identity = gamificationManager.decryptIdentity(id);
			String playerId = identity.getPlayerId();
			String campaignId = identity.getGameId();
			if(Utils.isNotEmpty(playerId) && Utils.isNotEmpty(campaignId)) {
				Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
				if(campaign != null) {
					CampaignPlayerSurvey survey = surveyRepository.findByPlayerIdAndCampaignIdAndSurveyName(playerId, campaignId, surveyName);
					info.setCompleted(survey.isCompleted());
					info.setUrl(survey.getSurveyLink().replace("playerId", id));
				}
			}
		} catch (Exception e) {
			logger.warn(String.format("getSurveyUrl error:%s - %s - %s", surveyName, id, e.getMessage()));
		}
		return info;
	}
	
}
