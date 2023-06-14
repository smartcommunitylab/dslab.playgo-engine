package it.smartcommunitylab.playandgo.engine.campaign.company;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.manager.azienda.PgAziendaleManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyRequest;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook.EventType;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.WebhookRequest;

@Component
public class CompanyCampaignSubscription {
	private static Logger logger = LoggerFactory.getLogger(CompanyCampaignSubscription.class);
	
	public static String companyKey = "companyKey";
	public static String employeeCode = "employeeCode";
	
	@Autowired
	PgAziendaleManager aziendaleManager;
	
	@Autowired
	MessageQueueManager queueManager;
	
    @Autowired
    SurveyManager surveyManager;

	public CampaignSubscription subscribeCampaign(Player player, Campaign campaign, 
			Map<String, Object> campaignData, boolean sendExtRequest) throws Exception {
		if(sendExtRequest) {
			aziendaleManager.subscribeCampaign(campaign.getCampaignId(), player.getPlayerId(), 
					(String)campaignData.get(companyKey), (String)campaignData.get(employeeCode));			
		}
		CampaignSubscription sub = new CampaignSubscription();
		sub.setPlayerId(player.getPlayerId());
		sub.setCampaignId(campaign.getCampaignId());
		sub.setTerritoryId(player.getTerritoryId());
		sub.setMail(player.getMail());
		sub.setSendMail(player.getSendMail());
		sub.setRegistrationDate(new Date());
		if(campaignData != null) {
			sub.setCampaignData(campaignData);
		}
        //check default survey
        if(campaign.hasDefaultSurvey()) {
            SurveyRequest sr = campaign.getDefaultSurvey();
            surveyManager.assignSurveyChallenges(campaign.getCampaignId(), Arrays.asList(player.getPlayerId()), sr);
        }            
		sendRegisterWebhookRequest(sub);
		return sub;			
	}
	
	public void unsubscribeCampaign(Player player, Campaign campaign) throws Exception {
		aziendaleManager.unsubscribeCampaign(campaign.getCampaignId(), player.getPlayerId());
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
