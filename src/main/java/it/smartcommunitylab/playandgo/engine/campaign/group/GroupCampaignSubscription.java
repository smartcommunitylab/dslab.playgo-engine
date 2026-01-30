package it.smartcommunitylab.playandgo.engine.campaign.group;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
	public static final String optionalClaimListKey = "optionalClaimList";

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
		
		if(!campaign.isRegistrationOpen(new Date())) {
			throw new ServiceException("Campaign registration is closed", ErrorCode.CAMPAIGN_REGISTRATION_CLOSED);
		}

		CampaignSubscription sub = new CampaignSubscription();
		sub.setPlayerId(player.getPlayerId());
		sub.setCampaignId(campaign.getCampaignId());
		sub.setTerritoryId(player.getTerritoryId());
		sub.setMail(player.getMail());
		sub.setSendMail(player.getSendMail());
		sub.setRegistrationDate(new Date());

	    String groupId = null;
        String extToken = null;
        if(campaignData != null) {
            if(campaignData.containsKey(groupIdKey)) {
                groupId = (String) campaignData.get(groupIdKey);
				sub.getCampaignData().put(groupIdKey, groupId);
            }
            if(campaignData.containsKey(externalTokenKey)) {
                extToken = (String) campaignData.get(externalTokenKey);
				sub.getCampaignData().put(externalTokenKey, extToken);
            }
        }
        
        // Validate JWT token if provided
        if(Utils.isNotEmpty(extToken)) {
            try {
                // Se il campaign ha un endpoint JWKS configurato, usalo per validare
				List<String> groupValues = extractGroupValues(campaign);
                String jwksEndpoint = (String) campaign.getSpecificData().get(jwksEndpointKey);
				String claimName = (String) campaign.getSpecificData().get(claimNameKey);
				if(Utils.isEmpty(jwksEndpoint))
					throw new ServiceException("JWKS endpoint not cofigured", ErrorCode.INVALID_TOKEN);
				Jwt jwt = jwtTokenUtil.validateAndGetClaimsWithJwks(extToken, jwksEndpoint);
				// Verifica che il valore del claim corrisponda al groupId fornito
				if(Utils.isEmpty(claimName))
					throw new ServiceException("Claim name not configured", ErrorCode.INVALID_TOKEN);
				String claimValue = jwt.getClaimAsString(claimName);
				if(Utils.isEmpty(claimValue)) {
					throw new ServiceException("Claim '" + claimName + "' not found in token", ErrorCode.INVALID_TOKEN);
				}
				// divido i valori di claimValue separati da , e verifico che tutti siano presenti in groupValues
				String[] claimValues = claimValue.split(",");
				boolean found = true;
				for(String cv : claimValues) {
					if(!groupValues.contains(cv)) {
						found = false;
						break;
					}
				}
				if(!found) {
					logger.warn("Claim value '{}' not in campaign group list", claimValue);
					throw new ServiceException("Group ID not valid for this campaign", ErrorCode.INVALID_TOKEN);
				}
				if(Utils.isEmpty(groupId)) {
					throw new ServiceException("Group ID is required", ErrorCode.INVALID_TOKEN);
				}
				// verifico che groupId sia uno di quelli presenti in claimValues
				boolean groupIdFound = false;
				for(String cv : claimValues) {
					if(cv.equals(groupId)) {
						groupIdFound = true;
						break;
					}
				}
				if(!groupIdFound) {
					logger.warn("Group ID '{}' not in claim values '{}'", groupId, claimValue);
					throw new ServiceException("Group ID does not match token claim", ErrorCode.INVALID_TOKEN);
				}
				// add optional claims to subscription data 
				if(campaign.getSpecificData().containsKey(optionalClaimListKey)) {
					@SuppressWarnings("unchecked")
					java.util.List<String> optionalClaims = (java.util.List<String>) campaign.getSpecificData().get(optionalClaimListKey);
					for(String claim : optionalClaims) {
						String cValue = jwt.getClaimAsString(claim);
						if(Utils.isNotEmpty(cValue)) {
							sub.getCampaignData().put(claim, cValue);
						}
					}
				}
            } catch (Exception e) {
                logger.error("Token JWT non valido: {}", e.getMessage());
                throw new ServiceException("External token validation failed", ErrorCode.INVALID_TOKEN);
            }
        } else {
			throw new ServiceException("External token is required", ErrorCode.INVALID_TOKEN);
		}

        if(!Utils.checkPlayerAlreadyRegistered(player, campaign)) {
            playerRepository.save(player);
            //check default survey
            if(campaign.hasDefaultSurvey()) {
                SurveyRequest sr = campaign.getDefaultSurvey();
				if(campaign.currentlyActive()) {
					surveyManager.assignSurveyChallenges(campaign.getCampaignId(), Arrays.asList(player.getPlayerId()), sr);
				} else {
					surveyManager.addSurveyTask(campaign.getCampaignId(), player.getPlayerId(), sr);
				}	
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

	/**
	 * Estrae una lista di valori dal campo "value" dalla groupList della campaign
	 * @param campaign oggetto Campaign
	 * @return lista di stringhe con i valori estratti
	 */
	@SuppressWarnings("unchecked")
	public java.util.List<String> extractGroupValues(Campaign campaign) {
		java.util.List<String> values = new java.util.ArrayList<>();
		
		if (campaign == null || campaign.getSpecificData() == null) {
			return values;
		}
		
		Object groupListObj = campaign.getSpecificData().get("groupList");
		if (groupListObj == null || !(groupListObj instanceof java.util.List)) {
			return values;
		}
		
		java.util.List<Map<String, Object>> groupList = (java.util.List<Map<String, Object>>) groupListObj;
		for (Map<String, Object> item : groupList) {
			Object value = item.get("value");
			if (value != null) {
				if(!values.contains(value.toString())) {
					values.add(value.toString());
				}
			}
		}
		
		return values;
	}

}
