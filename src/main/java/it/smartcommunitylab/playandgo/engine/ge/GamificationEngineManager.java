package it.smartcommunitylab.playandgo.engine.ge;

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

	@Value("${gamification.secretKey1}")
	private String secretKey1;
	
	@Value("${gamification.secretKey2}")
	private String secretKey2;

	ObjectMapper mapper = new ObjectMapper();
	
	EncryptDecrypt cryptUtils;
	
	@PostConstruct
	public void init() throws Exception {
		cryptUtils = new EncryptDecrypt(secretKey1, secretKey2);
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

	public void sendSaveItineraryAction(String playerId, String gameId, Map<String, Object> trackingData) {
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
		} catch (Exception e) {
			logger.error(String.format("sendSaveItineraryAction error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}		
	}
	
	public JsonNode getPlayerStatus(String gameId, String playerId) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/player/" + playerId;
			String json = HTTPConnector.doBasicAuthenticationGet(url, "application/json", "application/json", 
					gamificationUser, gamificationPassword);
			JsonNode jsonNode = mapper.readTree(json);
			return jsonNode;
		} catch (Exception e) {
			logger.error(String.format("getPlayerStatus error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return null;
	}
	
	public void assignSurveyChallenges(String playerId, String gameId, String surveyName, long start, long end, Map<String, Object> data) {
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
		} catch (Exception e) {
			logger.error(String.format("assignSurveyChallenges error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
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
	
	public void sendRecommendation(String playerId, String gameId) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("actionId", "app_sent_recommandation");
		data.put("gameId", gameId);
		data.put("playerId", playerId);
		data.put("data", new HashMap<String, Object>());
		String content = JsonUtils.toJSON(data);
		try {
			HTTPConnector.doBasicAuthenticationPost(gamificationUrl + "/gengine/execute", content, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
		} catch (Exception e) {
			logger.error(String.format("sendRecommendation error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
	}


}
