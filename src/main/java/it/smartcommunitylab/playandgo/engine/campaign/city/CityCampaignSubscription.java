package it.smartcommunitylab.playandgo.engine.campaign.city;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyRequest;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook.EventType;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.WebhookRequest;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class CityCampaignSubscription {
	private static Logger logger = LoggerFactory.getLogger(CityCampaignSubscription.class);
	
	public static String nickRecommendation = "nick_recommandation";
	public static String activePlayer = "activePlayer";
	
	@Autowired
	SurveyManager surveyManager;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	MessageQueueManager queueManager;
	
	public CampaignSubscription subscribeCampaign(Player player, Campaign campaign, 
			Map<String, Object> campaignData) throws Exception {
		
		CampaignSubscription sub = new CampaignSubscription();
		sub.setPlayerId(player.getPlayerId());
		sub.setCampaignId(campaign.getCampaignId());
		sub.setTerritoryId(player.getTerritoryId());
		sub.setMail(player.getMail());
		sub.setSendMail(player.getSendMail());
		sub.setRegistrationDate(new Date());
		if(!Utils.checkPlayerAlreadyRegistered(player, campaign)) {
		    playerRepository.save(player);
	        if(campaignData != null) {
	            sub.setCampaignData(campaignData);
	            //check player recommendation
	            if(campaignData.containsKey(nickRecommendation)) {
	                String nickname = (String) campaignData.get(nickRecommendation);
	                Player recommender = playerRepository.findByNickname(nickname);
	                if(recommender != null) {
	                    sub.getCampaignData().put(Campaign.recommenderPlayerId, recommender.getPlayerId());
	                    sub.getCampaignData().put(Campaign.recommendationPlayerToDo, Boolean.TRUE);
	                }
	            }               
	        }
	        //check default survey
	        if(campaign.hasDefaultSurvey()) {
	            SurveyRequest sr = campaign.getDefaultSurvey();
	            surveyManager.assignSurveyChallenges(campaign.getCampaignId(), Arrays.asList(player.getPlayerId()), sr);
	        }		    
		}
		//create player on GE
		if(Utils.isNotEmpty(campaign.getGameId())) {
			boolean createPlayer = gamificationEngineManager.createPlayer(player.getPlayerId(), campaign.getGameId());
			if(createPlayer) {
		        Map<String, Object> customData = new HashMap<>();
		        customData.put(activePlayer, true);
		        gamificationEngineManager.changeCustomData(player.getPlayerId(), campaign.getGameId(), customData);
			} else {
			    throw new ServiceException("GamificationEngine create user error", ErrorCode.EXT_SERVICE_INVOCATION); 
			}
		}
		sendRegisterWebhookRequest(sub);
		return sub;
	}
	
	public void unsubscribeCampaign(Player player, Campaign campaign) throws Exception {
        if(Utils.isNotEmpty(campaign.getGameId())) {
            Map<String, Object> customData = new HashMap<>();
            customData.put(activePlayer, false);
            gamificationEngineManager.changeCustomData(player.getPlayerId(), campaign.getGameId(), customData);
        }
		sendUnregisterWebhookRequest(player.getPlayerId(), campaign.getCampaignId());
	}
	
	private void sendRegisterWebhookRequest(CampaignSubscription sub) {
		WebhookRequest req = new  WebhookRequest();
		req.setCampaignId(sub.getCampaignId());
		req.setPlayerId(sub.getPlayerId());
		req.setEventType(EventType.register);
		req.getData().putAll(sub.getCampaignData());
		try {
			queueManager.sendCallWebhookRequest(req);
		} catch (Exception e) {
			logger.error("sendWebhookRequest:" + e.getMessage());
		}
	}
	
	private void sendUnregisterWebhookRequest(String playerId, String campaignId) {
		WebhookRequest req = new  WebhookRequest();
		req.setCampaignId(campaignId);
		req.setPlayerId(playerId);
		req.setEventType(EventType.unregister);
		try {
			queueManager.sendCallWebhookRequest(req);
		} catch (Exception e) {
			logger.error("sendWebhookRequest:" + e.getMessage());
		}
	}
	
}
