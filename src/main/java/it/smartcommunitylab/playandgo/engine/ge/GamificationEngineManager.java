package it.smartcommunitylab.playandgo.engine.ge;

import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.ge.model.ChallengeAssignmentDTO;
import it.smartcommunitylab.playandgo.engine.ge.model.ExecutionDataDTO;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerIdentity;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation;
import it.smartcommunitylab.playandgo.engine.util.EncryptDecrypt;
import it.smartcommunitylab.playandgo.engine.util.HTTPConnector;
import it.smartcommunitylab.playandgo.engine.util.JsonUtils;

@Component
public class GamificationEngineManager {
	private static transient final Logger logger = LoggerFactory.getLogger(GamificationEngineManager.class);
	
	public static final String SAVE_ITINERARY = "save_itinerary";
	public static final String START_TIME = "startTime";
	
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	@Value("${gamification.user}")
	private String gamificationUser;
	
	@Value("${gamification.password}")
	private String gamificationPassword;

	@Value("${playgoURL}")
	private String playgoURL;

	@Value("${gamification.secretKey1}")
	private String secretKey1;
	
	@Value("${gamification.secretKey2}")
	private String secretKey2;
	
	private static final String SURVEY_URL = "%s/survey/%s/%s/%s";
	private static final String UNSUBSCRIBE_URL = "%s/api/unsubscribeMail/%s";

	ObjectMapper mapper = new ObjectMapper();
	
	EncryptDecrypt cryptUtils;
	
	@PostConstruct
	public void init() throws Exception {
		cryptUtils = new EncryptDecrypt(secretKey1, secretKey2);
	}
	
	public String getPlaygoURL() {
		return playgoURL;
	}

	/**
	 * @param playerId
	 * @return identity corresponding to the string
	 * @throws Exception 
	 */
	public PlayerIdentity decryptIdentity(String value) throws Exception {
		String decrypted = cryptUtils.decrypt(value);
		String[] parts = decrypted.split(":");
		if (parts == null || parts.length != 2) throw new InvalidKeyException("Invalid identity content: "+decrypted);
		PlayerIdentity identity = new PlayerIdentity();
		identity.setPlayerId(parts[0]);
		identity.setGameId(parts[1]);
		return identity;
	}
	
	public String encryptIdentity(String playerId, String gameId) throws Exception {
		String id = cryptUtils.encrypt(playerId + ":" + gameId);
		return id;
	}
	
	/**
	 * Generate a survey URL
	 * @param playerId
	 * @param survey
	 * @param lang
	 * @return
	 * @throws Exception
	 */
	public String createSurveyUrl(String playerId, String gameId, String surveyName, String lang) throws Exception {
		String id = encryptIdentity(playerId, gameId);
		String compileSurveyUrl = String.format(SURVEY_URL, playgoURL, lang, surveyName, id);
		return compileSurveyUrl;
	}

	public boolean sendSaveItineraryAction(String playerId, String gameId, Map<String, Object> trackingData) {
		try {
			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(gameId);
			ed.setPlayerId(playerId);
			ed.setActionId(SAVE_ITINERARY);
			ed.setData(trackingData);
			
			Long time = (Long)trackingData.get(START_TIME);
			ed.setExecutionMoment(new Date(time));			

			String content = JsonUtils.toJSON(ed);
			
			HTTPConnector.doBasicAuthenticationPost(gamificationUrl + "/gengine/execute", content, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
			return true;
		} catch (Exception e) {
			logger.error(String.format("sendSaveItineraryAction error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}		
		return false;
	}
	
	public JsonNode getPlayerStatus(String gameId, String playerId, String points) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId
					+ "?readChallenges=false&points=" + points;
			String json = HTTPConnector.doBasicAuthenticationGet(url, "application/json", "application/json", 
					gamificationUser, gamificationPassword);
			JsonNode jsonNode = mapper.readTree(json);
			return jsonNode;
		} catch (Exception e) {
			logger.error(String.format("getPlayerStatus error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return null;
	}
	
	public boolean assignSurveyChallenges(String playerId, String gameId, String surveyName, long start, long end, Map<String, Object> data) {
		try {
			ChallengeAssignmentDTO challenge = new ChallengeAssignmentDTO();
			challenge.setStart(start);
			challenge.setEnd(end);
			challenge.setModelName("survey");
			challenge.setData(data);
			challenge.setInstanceName(surveyName + "_survey_" + System.currentTimeMillis() + "_" + playerId + "_" + gameId);
			
			String content = JsonUtils.toJSON(challenge);
			
			String url = gamificationUrl + "/gengine/game/" + gameId + "/player/" + playerId + "/challenges";
			HTTPConnector.doBasicAuthenticationPost(url, content, "application/json", "application/json", 
					gamificationUser, gamificationPassword);
			return true;
		} catch (Exception e) {
			logger.error(String.format("assignSurveyChallenges error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return false;
	}
	
	public boolean sendSurvey(String playerId, String gameId, String surveyName) {
		ExecutionDataDTO ed = new ExecutionDataDTO();
		ed.setGameId(gameId);
		ed.setPlayerId(playerId);
		ed.setActionId("survey_complete");
		ed.setData(Collections.singletonMap("surveyType", surveyName));

		String content = JsonUtils.toJSON(ed);
		try {
			HTTPConnector.doBasicAuthenticationPost(gamificationUrl + "/gengine/execute", content, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
			return true;
		} catch (Exception e) {
			logger.error(String.format("sendSurvey error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return false;
	}
	
	public boolean sendRecommendation(String playerId, String gameId) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("actionId", "app_sent_recommandation");
		data.put("gameId", gameId);
		data.put("playerId", playerId);
		data.put("data", new HashMap<String, Object>());
		String content = JsonUtils.toJSON(data);
		try {
			HTTPConnector.doBasicAuthenticationPost(gamificationUrl + "/gengine/execute", content, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
			return true;
		} catch (Exception e) {
			logger.error(String.format("sendRecommendation error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return false;
	}
	
	public String getChallengeStatus(String playerId, String gameId) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId + "/inventory";
			String json = HTTPConnector.doBasicAuthenticationGet(url, "application/json", "application/json", 
					gamificationUser, gamificationPassword);
			return json;
		} catch (Exception e) {
			logger.error(String.format("getChallengeStatus error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return null;		
	}

	public String activateChallengeByType(String playerId, String gameId, String name, String type) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("type", type);
		data.put("name", name);
		String content = JsonUtils.toJSON(data);
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId + "/inventory/activate";
			String json = HTTPConnector.doBasicAuthenticationPost(url, content, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
			return json;
		} catch (Exception e) {
			logger.error(String.format("activateChallengeByType error: %s - %s - %s - %s - %s", gameId, playerId, name, type, e.getMessage()));
		}
		return null;
	}
	
	public String getGameStatus(String playerId, String gameId) {
		try {
			String url = gamificationUrl + "/gengine/state/" + gameId + "/" + playerId;
			String json = HTTPConnector.doBasicAuthenticationGet(url, "application/json", "application/json", 
					gamificationUser, gamificationPassword);
			return json;
		} catch (Exception e) {
			logger.error(String.format("getGameStatus error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return null;		
	}
	
	public boolean chooseChallenge(String playerId, String gameId, String challengeId) {
		try {
			String url = gamificationUrl + "/gengine/game/" + gameId + "/player/" + playerId + "/challenges/" + challengeId + "/accept";
			HTTPConnector.doBasicAuthenticationPost(url, null, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
			return true;
		} catch (Exception e) {
			logger.error(String.format("chooseChallenge error: %s - %s - %s - %s", gameId, playerId, challengeId, e.getMessage()));
		}
		return false;
	}
	
	public boolean sendChallengeInvitation(String playerId, String gameId, ChallengeInvitation ci) {
		try {
			String content = JsonUtils.toJSON(ci);
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId + "/invitation";
			HTTPConnector.doBasicAuthenticationPost(url, content, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
			return true;
		} catch (Exception e) {
			logger.error(String.format("sendChallengeInvitation error: %s - %s - %s - %s", gameId, playerId, ci.getChallengeName(), e.getMessage()));
		}
		return false;		
	}
	
	public boolean changeChallengeInvitationStatus(String playerId, String gameId, String challengeName, String status) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId + "/invitation/" 
					+ status + "/" + URLEncoder.encode(challengeName, "UTF-8");
			HTTPConnector.doBasicAuthenticationPost(url, null, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
			return true;
		} catch (Exception e) {
			logger.error(String.format("changeChallengeInvitationStatus error: %s - %s - %s - %s", gameId, playerId, challengeName, e.getMessage()));
		}
		return false;		
	}
	
	public String getChallengables(String playerId, String gameId) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId + "/challengers";
			String json = HTTPConnector.doBasicAuthenticationGet(url, "application/json", "application/json", 
					gamificationUser, gamificationPassword);
			return json;
		} catch (Exception e) {
			logger.error(String.format("getChallengables error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return null;		
	}
	
	public String getStatistics(String gameId) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/statistics";
			String json = HTTPConnector.doBasicAuthenticationGet(url, "application/json", "application/json", 
					gamificationUser, gamificationPassword);
			return json;
		} catch (Exception e) {
			logger.error(String.format("getStatistics error: %s - %s", gameId, e.getMessage()));
		}
		return null;				
	}
	
	public String getNotifications(String playerId, String gameId) {
		try {
			String url = gamificationUrl + "/notification/game/" + gameId + "/player/" + playerId + "/grouped?size=10000";
			String json = HTTPConnector.doBasicAuthenticationGet(url, "application/json", "application/json", 
					gamificationUser, gamificationPassword);
			return json;
		} catch (Exception e) {
			logger.error(String.format("getStatistics error: %s - %s", gameId, e.getMessage()));
		}
		return null;						
	}
	
	public String getBlackList(String playerId, String gameId) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId + "/blacklist";
			String json = HTTPConnector.doBasicAuthenticationGet(url, "application/json", "application/json", 
					gamificationUser, gamificationPassword);
			return json;
		} catch (Exception e) {
			logger.error(String.format("getBlackList error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return null;								
	}
	
	public boolean addToBlackList(String playerId, String gameId, String blockedPlayerId) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId + "/block/" + blockedPlayerId;
			HTTPConnector.doBasicAuthenticationPost(url, null, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
			return true;
		} catch (Exception e) {
			logger.error(String.format("addToBlackList error: %s - %s - %s - %s", gameId, playerId, blockedPlayerId, e.getMessage()));
		}
		return false;				
	}
	
	public boolean deleteFromBlackList(String playerId, String gameId, String blockedPlayerId) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId + "/unblock/" + blockedPlayerId;
			HTTPConnector.doBasicAuthenticationPost(url, null, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
			return true;
		} catch (Exception e) {
			logger.error(String.format("deleteFromBlackList error: %s - %s - %s - %s", gameId, playerId, blockedPlayerId, e.getMessage()));
		}
		return false;						
	}
	
}
