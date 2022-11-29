package it.smartcommunitylab.playandgo.engine.campaign.school;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.manager.highschool.PgHighSchoolManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyRequest;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook.EventType;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.WebhookRequest;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class SchoolCampaignSubscription {
	private static Logger logger = LoggerFactory.getLogger(SchoolCampaignSubscription.class);
	
	public static final String groupIdKey = "teamId";
	
	@Autowired
	SurveyManager surveyManager;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;
	
	@Autowired
	MessageQueueManager queueManager;
	
	@Autowired
	PgHighSchoolManager highSchoolManager;

	public CampaignSubscription subscribeCampaign(Player player, Campaign campaign, 
			Map<String, Object> campaignData, boolean sendExtRequest) throws Exception {
	    String groupId = null;
	    if(sendExtRequest) {
	        groupId = highSchoolManager.subscribeCampaign(campaign.getCampaignId(), player.getPlayerId(), player.getNickname());
	    }
		CampaignSubscription sub = new CampaignSubscription();
		sub.setPlayerId(player.getPlayerId());
		sub.setCampaignId(campaign.getCampaignId());
		sub.setTerritoryId(player.getTerritoryId());
		sub.setMail(player.getMail());
		sub.setSendMail(player.getSendMail());
		sub.setRegistrationDate(new Date());
		if(campaignData != null) {
			sub.getCampaignData().putAll(campaignData);
		}
        sub.getCampaignData().put(groupIdKey, groupId);
		//check default survey
		if(campaign.hasDefaultSurvey()) {
			SurveyRequest sr = campaign.getDefaultSurvey();
			surveyManager.assignSurveyChallenges(campaign.getCampaignId(), Arrays.asList(player.getPlayerId()), sr);
		}
		//create player on GE
		if(Utils.isNotEmpty(campaign.getGameId())) {
			gamificationEngineManager.createPlayer(player.getPlayerId(), campaign.getGameId());
		}
		sendRegisterWebhookRequest(sub);
		return sub;
	}
	
	public void unsubscribeCampaign(Player player, Campaign campaign) throws Exception {
	    highSchoolManager.unsubscribeCampaign(campaign.getCampaignId(), player.getPlayerId());
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
