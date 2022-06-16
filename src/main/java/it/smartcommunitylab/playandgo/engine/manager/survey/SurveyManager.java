package it.smartcommunitylab.playandgo.engine.manager.survey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.ge.PlayerIdentity;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerSurvey;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerSurveyRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class SurveyManager {
	private static transient final Logger logger = LoggerFactory.getLogger(SurveyManager.class);

	@Autowired
	GamificationEngineManager gamificationManager;
	
	@Autowired
	CampaignRepository campaignRepository;

	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	

	@Autowired
	CampaignPlayerSurveyRepository surveyRepository;
	
	public void assignSurveyChallenges(String campaignId, List<String> playerIds, SurveyRequest sr) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);			
		}
		Map<String, Object> data = new HashMap<String, Object>(sr.getData());
		data.put("surveyType", sr.getSurveyName());
		data.put("link", "");
		// Force double for bonus score
		if (data.containsKey("bonusScore")) {
			data.put("bonusScore", Double.parseDouble(data.get("bonusScore").toString()));
		}
		
		if(playerIds == null || playerIds.size() == 0) {
			playerIds = new ArrayList<>();
			List<CampaignSubscription> campaignSubscriptions = campaignSubscriptionRepository.findByCampaignId(campaignId);
			for(CampaignSubscription cs : campaignSubscriptions) {
				playerIds.add(cs.getPlayerId());
			}
		}
		
		for(String playerId : playerIds) {
			gamificationManager.assignSurveyChallenges(playerId, campaign.getGameId(), sr.getSurveyName(), 
					sr.getStart(), sr.getEnd(), data);
		}
	}
	
	public boolean compileSurvey(String surveyName, Map<String,Object> formData) {
		boolean complete = false;
		try {
			String id = (String)formData.get("playerId");
			PlayerIdentity identity = gamificationManager.decryptIdentity(id);
			String playerId = identity.getPlayerId();
			String gameId = identity.getGameId();
			if(Utils.isNotEmpty(playerId) && Utils.isNotEmpty(gameId)) {
				Campaign campaign = campaignRepository.findByGameId(gameId);
				if(campaign == null) {
					return false;
				}
				CampaignPlayerSurvey survey = surveyRepository.findByPlayerIdAndGameIdAndSurveyName(playerId, gameId, surveyName);
				if(survey != null) {
					return true;
				}
				if(campaign.getAllSurveys().containsKey(surveyName)) {
					survey = new CampaignPlayerSurvey();
					survey.setPlayerId(playerId);
					survey.setGameId(gameId);
					survey.setCampaignId(campaign.getCampaignId());
					survey.setTimestamp(Utils.getUTCDate(System.currentTimeMillis()));
					survey.setSurveyLink(campaign.getAllSurveys().get(surveyName));
					survey.setSurveyName(surveyName);
					
					Map<String, Object> data = new HashMap<>(formData);
					data.remove("playerId");
					data = correctData(data);
					complete = gamificationManager.sendSurvey(playerId, gameId, surveyName);
					if(complete) {
						surveyRepository.save(survey);
					}
				}
			}			
		} catch (Exception e) {
			logger.warn(String.format("compileSurvey error:%s - %s - %s", surveyName, formData, e.getMessage()));
		}
		return complete;
	}
	
	private Map<String, Object> correctData(Map<String, Object> data) {
		Map<String, Object> res = new HashMap<>();
		data.keySet().forEach(k -> {
			String nk = k.replace('.', ' ');
			res.put(nk, data.getOrDefault(k, ""));
		});
		return res;
	}

	public SurveyInfo getSurveyUrl(String id, String surveyName) {
		SurveyInfo info = new SurveyInfo();
		try {
			PlayerIdentity identity = gamificationManager.decryptIdentity(id);
			String playerId = identity.getPlayerId();
			String gameId = identity.getGameId();
			if(Utils.isNotEmpty(playerId) && Utils.isNotEmpty(gameId)) {
				Campaign campaign = campaignRepository.findByGameId(gameId);
				if(campaign != null) {
					CampaignPlayerSurvey survey = surveyRepository.findByPlayerIdAndGameIdAndSurveyName(playerId, gameId, surveyName);
					if(survey != null) {
						info.setCompleted(true);
					} else {
						if(campaign.getAllSurveys().containsKey(surveyName)) {
							info.setUrl(campaign.getAllSurveys().get(surveyName));
						}
					}
				}
			}
		} catch (Exception e) {
			logger.warn(String.format("getSurveyUrl error:%s - %s - %s", surveyName, id, e.getMessage()));
		}
		return info;
	}
	
}
