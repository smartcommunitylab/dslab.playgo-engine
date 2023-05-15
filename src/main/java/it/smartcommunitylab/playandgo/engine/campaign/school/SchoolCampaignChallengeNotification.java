package it.smartcommunitylab.playandgo.engine.campaign.school;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.manager.ChallengeStatsManager;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeManager;
import it.smartcommunitylab.playandgo.engine.model.PlayerChallenge;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatChallengeRepository;

@Component
public class SchoolCampaignChallengeNotification {
	private static final Logger logger = LoggerFactory.getLogger(SchoolCampaignChallengeNotification.class);
	
	@Autowired
	PlayerStatChallengeRepository playerStatChallengeRepository;
	
	@Autowired
	ChallengeStatsManager challengeStatsManager;
	
	@Autowired
	ChallengeManager challengeManager;
	
	public void challengeCompleted(Map<String, Object> msg) {
		challengeStatus(msg);
	}
	
	public void challengeFailed(Map<String, Object> msg) {
		challengeStatus(msg);
	}
	
	@SuppressWarnings("rawtypes")
	private void challengeStatus(Map<String, Object> msg) {
		String type = (String) msg.get("type");
		boolean completed = false;
		if(type.endsWith("ChallengeCompletedNotication")) {
			completed = true;
		}
		Map obj = (Map) msg.get("obj");
		if(obj.containsKey("gameId")) {
			String gameId = (String) obj.get("gameId");
			String playerId = (String) obj.get("playerId");
			String model = (String) obj.get("model");
			String challengeName = (String) obj.get("challengeName");
			String counterName = (String) obj.get("pointConcept");
			//long timestamp = (Long) obj.get("timestamp");
			//long start = (Long) obj.get("start");
			long timestamp = (Long) obj.get("end");
			try {
				PlayerChallenge playerChallenge = challengeManager.storePlayerChallenge(playerId, gameId, challengeName);
				if((playerChallenge.getChallengeData() != null) && (playerChallenge.getChallengeData().getChallCompletedDate() > 0)) {
					timestamp = playerChallenge.getChallengeData().getChallCompletedDate();
				}
			} catch (Exception e) {
				logger.error(String.format("challengeStatus storePlayerChallenge [%s - %s - %s]:%s", playerId, gameId, challengeName, e.getMessage()));
			}
			challengeStatsManager.updateChallengeStat(playerId, gameId, model, challengeName, counterName, timestamp, completed);			
		}
	}
}
