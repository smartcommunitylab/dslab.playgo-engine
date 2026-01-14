package it.smartcommunitylab.playandgo.engine.campaign.group;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
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
import it.smartcommunitylab.playandgo.engine.util.JwtTokenUtil;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class GroupCampaignSubscription {
    private static Logger logger = LoggerFactory.getLogger(GroupCampaignSubscription.class);

    public static final String groupIdKey = "groupId";
    public static final String externalTokenKey = "extToken";
	
	public static final String jwksEndpointKey = "jwksEndpoint";
	public static final String claimNameKey = "claimName";
    public static final String claimRegExpKey = "claimRegExp";

    @Autowired
    PlayerRepository playerRepository;

	@Autowired
	SurveyManager surveyManager;

	@Autowired
	MessageQueueManager queueManager;

	@Autowired
	GamificationEngineManager gamificationEngineManager;

	@Autowired
	JwtTokenUtil jwtTokenUtil;

	public CampaignSubscription subscribeCampaign(Player player, Campaign campaign, 
			Map<String, Object> campaignData) throws Exception {
	    String groupId = null;
        String extToken = null;
        if(campaignData != null) {
            if(campaignData.containsKey(groupIdKey)) {
                groupId = (String) campaignData.get(groupIdKey);
            }
            if(campaignData.containsKey(externalTokenKey)) {
                extToken = (String) campaignData.get(externalTokenKey);
            }
        }
        
        // Validate JWT token if provided
        if(Utils.isNotEmpty(extToken)) {
            try {
                // Se il campaign ha un endpoint JWKS configurato, usalo per validare
                String jwksEndpoint = (String) campaign.getSpecificData().get(jwksEndpointKey);
				String claimName = (String) campaign.getSpecificData().get(claimNameKey);
				String claimRegExp = (String)campaign.getSpecificData().get(claimRegExpKey); 
				if(Utils.isEmpty(jwksEndpoint))
					throw new ServiceException("JWKS endpoint not cofigured", ErrorCode.INVALID_TOKEN);
				Jwt jwt = jwtTokenUtil.validateAndGetClaimsWithJwks(extToken, jwksEndpoint);
				// Verifica la regola del provider se specificata
				if(Utils.isNotEmpty(claimRegExp)) {
					String claimValue = jwt.getClaimAsString(claimName);
					if(Utils.isEmpty(claimValue)) {
						throw new ServiceException("Claim '" + claimName + "' not found in token", ErrorCode.INVALID_TOKEN);
					}	
					// Verifica se il valore del claim matcha la regex
					if(!claimValue.matches(claimRegExp)) {
						logger.warn("Claim value '{}' does not match regex pattern '{}'", claimValue, claimRegExp);
						throw new ServiceException("Provider rule not satisfied", ErrorCode.INVALID_TOKEN);
					}
					logger.debug("Claim '{}' validated against regex pattern", claimName);
				}
            } catch (Exception e) {
                logger.error("Token JWT non valido: {}", e.getMessage());
                throw new ServiceException("External token validation failed", ErrorCode.INVALID_TOKEN);
            }
        }

		CampaignSubscription sub = new CampaignSubscription();
		sub.setPlayerId(player.getPlayerId());
		sub.setCampaignId(campaign.getCampaignId());
		sub.setTerritoryId(player.getTerritoryId());
		sub.setMail(player.getMail());
		sub.setSendMail(player.getSendMail());
		sub.setRegistrationDate(new Date());
        sub.getCampaignData().put(groupIdKey, groupId);
        sub.getCampaignData().put(externalTokenKey, extToken); 

        if(!Utils.checkPlayerAlreadyRegistered(player, campaign)) {
            playerRepository.save(player);
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
		        customData.put("activePlayer", true);
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
            customData.put("activePlayer", false);
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
