package it.smartcommunitylab.playandgo.engine.ge;

import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
	
	public void sendSaveItineraryAction(String playerId, String gameId, Map<String, Object> trackingData) {
		try {
			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(gameId);
			ed.setPlayerId(playerId);
			ed.setActionId(SAVE_ITINERARY);
			ed.setData(trackingData);
			
			Long time = (Long)trackingData.remove(START_TIME);
			ed.setExecutionMoment(new Date(time));			

			String content = JsonUtils.toJSON(ed);
			
			logger.debug("Sending to " + gamificationUrl + "/gengine/execute (" + SAVE_ITINERARY + ") = " + trackingData);
			HTTPConnector.doBasicAuthenticationPost(gamificationUrl + "/gengine/execute", content, "application/json", 
					"application/json", gamificationUser, gamificationPassword);
		} catch (Exception e) {
			logger.error("Error sending gamification action: " + e.getMessage());
		}		
	}


}
