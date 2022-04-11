package it.smartcommunitylab.playandgo.engine.ge;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerGameStatusRepository;
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
	PlayerGameStatusRepository playerGameStatusRepository;
	
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
						PlayerGameStatus gameStatus = playerGameStatusRepository.findByPlayerIdAndCampaignId(playerId, campaignId);
						if(gameStatus == null) {
							gameStatus = new PlayerGameStatus();
							gameStatus.setPlayerId(playerId);
							gameStatus.setCampaignId(campaignId);
							playerGameStatusRepository.save(gameStatus);
						}
						updatePlayerState(playerState, gameStatus);
						gameStatus.setUpdateTime(new Date());
						playerGameStatusRepository.save(gameStatus);
					}
				}
			}			
		} catch (Exception e) {
			logger.error(String.format("updatePlayerGameStatus error:%s - %s", e.getMessage(), msg));
		}
	}
	
	private void updatePlayerState(JsonNode root, PlayerGameStatus statusGame) throws Exception {
		//score
		JsonNode concepts = root.findPath("PointConcept");
		for(JsonNode pointConcept : concepts) {
			if(pointConcept.path("name").asText().equals("green leaves")) {
				statusGame.setScore(pointConcept.path("score").asDouble());
				
				//update generale
				PlayerStatsGame statsGlobal = statsGameRepository.findGlobalByPlayerIdAndCampaignId(
						statusGame.getPlayerId(), statusGame.getCampaignId());
				if(statsGlobal == null) {
					statsGlobal = new PlayerStatsGame();
					statsGlobal.setPlayerId(statusGame.getPlayerId());
					statsGlobal.setCampaignId(statusGame.getCampaignId());
					statsGlobal.setGlobal(Boolean.TRUE);
					statsGameRepository.save(statsGlobal);
				}
				statsGlobal.setScore(pointConcept.path("score").asDouble());
				statsGameRepository.save(statsGlobal);
				
				//update weekly points
				try {
					JsonNode weekly = pointConcept.findPath("weekly");
					JsonNode instances  = weekly.path("instances");
					List<String> dates = new ArrayList<>();
					for(Iterator<String> fields = instances.fieldNames(); fields.hasNext();) {
						dates.add(fields.next());
					}
					for(String weekDate : dates) {
						LocalDate day = LocalDate.parse(weekDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
						PlayerStatsGame statsGame = statsGameRepository.findByPlayerIdAndCampaignIdAndDayAndGlobal(
								statusGame.getPlayerId(), statusGame.getCampaignId(), day, Boolean.FALSE);
						if(statsGame == null) {
							statsGame = new PlayerStatsGame();
							statsGame.setPlayerId(statusGame.getPlayerId());
							statsGame.setCampaignId(statusGame.getCampaignId());
							statsGame.setGlobal(Boolean.FALSE);
							statsGame.setDay(day);
							int weekOfYear = day.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
							int monthOfYear = day.get(ChronoField.MONTH_OF_YEAR);
							int year = day.get(ChronoField.YEAR);
							statsGame.setWeekOfYear(year + "-" + weekOfYear);
							statsGame.setMonthOfYear(year + "-" + monthOfYear);
							statsGameRepository.save(statsGame);
						}
						statsGame.setScore(instances.path(weekDate).path("score").asDouble());
						statsGameRepository.save(statsGame);
					}
					
				} catch (Exception e) {
					logger.warn("updatePlayerState error:" + e.getMessage());
				}
			}
		}
		//level
		statusGame.getLevel().clear();
		JsonNode levels = root.path("levels");
		for(JsonNode level : levels) {
			if(level.path("pointConcept").asText().equals("green leaves")) {
				statusGame.getLevel().put("levelName", level.path("levelName").asText());
				statusGame.getLevel().put("levelValue", level.path("levelValue").asText());
				statusGame.getLevel().put("startLevelScore", level.path("startLevelScore").asDouble());
				statusGame.getLevel().put("endLevelScore", level.path("endLevelScore").asDouble());
				statusGame.getLevel().put("toNextLevel", level.path("toNextLevel").asDouble());
			}
		}
		//badges
		statusGame.getBadges().clear();
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
			statusGame.getBadges().add(badgeMap);
		}
		//challenges
		statusGame.getChallenges().clear();
		JsonNode challenges = root.findPath("ChallengeConcept");
		for(JsonNode challenge : challenges) {
			Map<String, Object> map = mapper.convertValue(challenge, new TypeReference<Map<String, Object>>(){});
			statusGame.getChallenges().add(map);
		}
	}
}
