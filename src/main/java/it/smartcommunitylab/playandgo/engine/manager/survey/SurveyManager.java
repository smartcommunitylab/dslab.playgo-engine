package it.smartcommunitylab.playandgo.engine.manager.survey;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.campaign.BasicCampaignSurveyManager;
import it.smartcommunitylab.playandgo.engine.campaign.company.CompanyCampaignSurveyManager;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerIdentity;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class SurveyManager {
	private static transient final Logger logger = LoggerFactory.getLogger(SurveyManager.class);

	@Autowired
	BasicCampaignSurveyManager basicCampaignSurveyManager;
	
	@Autowired
	CompanyCampaignSurveyManager companyCampaignSurveyManager;
	
	@Autowired
	CampaignRepository campaignRepository;

	@Autowired
    GamificationEngineManager gamificationManager;
	
	public void assignSurveyChallenges(String campaignId, List<String> playerIds, SurveyRequest sr) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);			
		}
		switch (campaign.getType()) {
		    case company:
		        companyCampaignSurveyManager.assignSurvey(campaignId, playerIds, sr);
		        break;
		    case city:
		        basicCampaignSurveyManager.assignSurvey(campaignId, playerIds, sr);
		        break;
		    case school:
		        basicCampaignSurveyManager.assignSurvey(campaignId, playerIds, sr);
		        break;
            default:
                break;
		}
	}
	
	public boolean compileSurvey(String surveyName, Map<String,Object> formData) {
		boolean complete = false;
		try {
			String id = (String)formData.getOrDefault("playerId", (String)formData.get("AuthorizationCode"));
			PlayerIdentity identity = gamificationManager.decryptIdentity(id);
			String playerId = identity.getPlayerId();
			String gameId = identity.getGameId();
            logger.info("compileSurvey:" + surveyName + " - " + playerId);          			
			if(Utils.isNotEmpty(playerId) && Utils.isNotEmpty(gameId)) {
			    Campaign campaign = getCampaign(gameId);
			    if(campaign == null) {
			        return false;
		        }
		        switch (campaign.getType()) {
		            case company:
		                complete = companyCampaignSurveyManager.compileSurvey(surveyName, formData);
		                break;
		            case city:
		                complete = basicCampaignSurveyManager.compileSurvey(surveyName, formData);
		                break;
		            case school:
		                complete = basicCampaignSurveyManager.compileSurvey(surveyName, formData);
		                break;
		            default:
		                break;			                
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
			String gameId = identity.getGameId();
			if(Utils.isNotEmpty(playerId) && Utils.isNotEmpty(gameId)) {
				Campaign campaign = getCampaign(gameId);
				if(campaign != null) {
	                switch (campaign.getType()) {
	                    case company:
	                        info = companyCampaignSurveyManager.getSurveyUrl(id, surveyName);
	                        break;
	                    case city:
	                        info = basicCampaignSurveyManager.getSurveyUrl(id, surveyName);
	                        break;
	                    case school:
	                        info = basicCampaignSurveyManager.getSurveyUrl(id, surveyName);
	                        break;
	                    default:
	                        break;                          
	                }   				    
				}
			}
		} catch (Exception e) {
			logger.warn(String.format("getSurveyUrl error:%s - %s - %s", surveyName, id, e.getMessage()));
		}
		return info;
	}
	
	private Campaign getCampaign(String id) {
	    Campaign campaign = campaignRepository.findByGameId(id);
	    if(campaign == null) {
            campaign = campaignRepository.findById(id).orElse(null);
	    }
	    return campaign;
	}
	
}
