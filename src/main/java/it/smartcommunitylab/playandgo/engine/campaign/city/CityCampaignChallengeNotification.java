package it.smartcommunitylab.playandgo.engine.campaign.city;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.manager.ChallengeStatsManager;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatChallengeRepository;

@Component
public class CityCampaignChallengeNotification {
	
	@Autowired
	PlayerStatChallengeRepository playerStatChallengeRepository;
	
	@Autowired
	ChallengeStatsManager challengeStatsManager;
	
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
			long end = (Long) obj.get("end");
			challengeStatsManager.updateChallengeStat(playerId, gameId, model, challengeName, counterName, end, completed);			
		}
	}
}
