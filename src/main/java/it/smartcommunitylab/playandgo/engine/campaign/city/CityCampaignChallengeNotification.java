package it.smartcommunitylab.playandgo.engine.campaign.city;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatChallenge;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatChallengeRepository;

@Component
public class CityCampaignChallengeNotification {
	
	@Autowired
	PlayerStatChallengeRepository playerStatChallengeRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	public void challengeCompleted(Map<String, Object> msg) {
		challengeStatus(msg);
	}
	
	public void challengeFailed(Map<String, Object> msg) {
		challengeStatus(msg);
	}
	
	@SuppressWarnings("rawtypes")
	private void challengeStatus(Map<String, Object> msg) {
		PlayerStatChallenge stat = new PlayerStatChallenge();
		String type = (String) msg.get("type");
		if(type.equalsIgnoreCase("challenge_complete")) {
			stat.setComplete(Boolean.TRUE);
		}
		Map obj = (Map) msg.get("obj");
		if(obj.containsKey("gameId")) {
			String gameId = (String) obj.get("gameId");
			Campaign campaign = campaignRepository.findByGameId(gameId);
			stat.setCampaignId(campaign.getCampaignId());
			stat.setPlayerId((String) obj.get("playerId"));
			stat.setTimestamp((Long) obj.get("timestamp"));
			stat.setChallengeName((String) obj.get("challengeName"));
			//TODO type and unit
			playerStatChallengeRepository.save(stat);
		}
	}
}
