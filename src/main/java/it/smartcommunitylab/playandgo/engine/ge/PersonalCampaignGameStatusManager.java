package it.smartcommunitylab.playandgo.engine.ge;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsGameRepository;

@Component
public class PersonalCampaignGameStatusManager {
	private static transient final Logger logger = LoggerFactory.getLogger(PersonalCampaignGameStatusManager.class);
	
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	PlayerStatsGameRepository statsGameRepository;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;
	
	ObjectMapper mapper = new ObjectMapper();

	public void updatePlayerGameStatus(Map<String, Object> msg) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> obj = (Map<String, Object>) msg.get("obj");
			if(obj != null) {
				String gameId = (String) obj.get("gameId");
				String playerId = (String) obj.get("playerId");
				double delta = (double) obj.get("delta");
				double score = (double) obj.get("score");
				String trackId = null;
				@SuppressWarnings("unchecked")
				Map<String, Object> dataPayLoad = (Map<String, Object>) obj.get("dataPayLoad");
				if(dataPayLoad != null) {
					trackId = (String) dataPayLoad.get("trackId");
				}
				Campaign campaign = campaignRepository.findByGameId(gameId);
				if(campaign != null) {
					String campaignId = campaign.getCampaignId();
					
					CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(playerId, campaignId, trackId);
					if(playerTrack != null) {
						playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
						playerTrack.setScore(delta);
						campaignPlayerTrackRepository.save(playerTrack);
					}
					
					JsonNode playerState = gamificationEngineManager.getPlayerStatus(gameId, playerId);
					if(playerState != null) {
						PlayerStatsGame statsGame = statsGameRepository.findByPlayerIdAndCampaignId(playerId, campaignId);
						if(statsGame == null) {
							statsGame = new PlayerStatsGame();
							statsGame.setPlayerId(playerId);
							statsGame.setCampaignId(campaignId);
							statsGameRepository.save(statsGame);
						}
						updatePlayerState(playerState, statsGame);
						statsGame.setUpdateTime(new Date());
						statsGame.setScore(score);
						statsGameRepository.save(statsGame);
					}
				}
			}			
		} catch (Exception e) {
			logger.error(String.format("updatePlayerGameStatus error:%s - %s", e.getMessage(), msg));
		}
	}
	
	private void updatePlayerState(JsonNode root, PlayerStatsGame statsGame) throws Exception {
		//score
		JsonNode concepts = root.findPath("PointConcept");
		for(JsonNode pointConcept : concepts) {
			if(pointConcept.path("name").asText().equals("green leaves")) {
				statsGame.setScore(pointConcept.path("score").asDouble());
				JsonNode weekly = pointConcept.findPath("weekly");
				JsonNode instances  = weekly.path("instances");
				List<String> dates = new ArrayList<>();
				for(Iterator<String> fields = instances.fieldNames(); fields.hasNext();) {
					dates.add(fields.next());
				}
				if(dates.size() == 1) {
					JsonNode node = instances.path(dates.get(0));
					statsGame.setWeeklyScore(node.path("score").asDouble());
				} else if(dates.size() == 2) {
					String date1 = dates.get(0);
					String date2 = dates.get(1);
					JsonNode node1 = instances.path(date1);
					JsonNode node2 = instances.path(date2);
					if(date1.compareTo(date2) > 0) {
						statsGame.setWeeklyScore(node1.path("score").asDouble());
						statsGame.setPreviousWeeklyScore(node2.path("score").asDouble());
					} else {
						statsGame.setWeeklyScore(node2.path("score").asDouble());
						statsGame.setPreviousWeeklyScore(node1.path("score").asDouble());						
					}
				}
			}
		}
		//level
		statsGame.getLevel().clear();
		JsonNode levels = root.path("levels");
		for(JsonNode level : levels) {
			if(level.path("pointConcept").asText().equals("green leaves")) {
				statsGame.getLevel().put("levelName", level.path("levelName").asText());
				statsGame.getLevel().put("levelValue", level.path("levelValue").asText());
				statsGame.getLevel().put("startLevelScore", level.path("startLevelScore").asDouble());
				statsGame.getLevel().put("endLevelScore", level.path("endLevelScore").asDouble());
				statsGame.getLevel().put("toNextLevel", level.path("toNextLevel").asDouble());
			}
		}
		//badges
		statsGame.getBadges().clear();
		JsonNode badges = root.findPath("BadgeCollectionConcept");
		for(JsonNode badge : badges) {
			Map<String, Object> badgeMap = new HashMap<>();
			badgeMap.put("name", badge.path("name").asText());
			List<String> badgeEarned = new ArrayList<>();
			JsonNode list = badge.path("badgeEarned");
			for(JsonNode b : list) {
				badgeEarned.add(b.asText());
			}
			badgeMap.put("badgeEarned", badgeEarned);
			statsGame.getBadges().add(badgeMap);
		}
		//challenges
		statsGame.getChallenges().clear();
		JsonNode challenges = root.findPath("ChallengeConcept");
		for(JsonNode challenge : challenges) {
			Map<String, Object> map = mapper.convertValue(challenge, new TypeReference<Map<String, Object>>(){});
			statsGame.getChallenges().add(map);
		}
	}
}
