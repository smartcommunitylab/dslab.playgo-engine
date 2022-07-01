package it.smartcommunitylab.playandgo.engine.ge;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerGameStatusRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsGameRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;

@Component
public class BasicCampaignGameStatusManager {
	private static transient final Logger logger = LoggerFactory.getLogger(BasicCampaignGameStatusManager.class);
	
	@Autowired
	TerritoryRepository territoryRepository;
	
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	PlayerStatsGameRepository statsGameRepository;
	
	@Autowired
	PlayerGameStatusRepository playerGameStatusRepository;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;
	
	ObjectMapper mapper = new ObjectMapper();
	
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
					
					CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(playerId, 
							campaignId, trackId);
					if(playerTrack != null) {
						playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
						playerTrack.setScore(playerTrack.getScore() + delta);
						campaignPlayerTrackRepository.save(playerTrack);
					}
					
					PlayerGameStatus gameStatus = playerGameStatusRepository.findByPlayerIdAndCampaignId(playerId, campaignId);
					if(gameStatus == null) {
						Player p = playerRepository.findById(playerId).orElse(null);
						gameStatus = new PlayerGameStatus();
						gameStatus.setPlayerId(playerId);
						gameStatus.setNickname(p.getNickname());
						gameStatus.setCampaignId(campaignId);
						playerGameStatusRepository.save(gameStatus);
					}
					
					//update daily points
					try {
						ZonedDateTime trackDay = getTrackDay(campaign, playerTrack);
						String day = trackDay.format(dtf);
						int weekOfYear = trackDay.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
						int monthOfYear = trackDay.get(ChronoField.MONTH_OF_YEAR);
						int year = trackDay.get(ChronoField.YEAR);
						PlayerStatsGame statsGame = statsGameRepository.findByPlayerIdAndCampaignIdAndDayAndGlobal(playerId, campaignId, 
								day, Boolean.FALSE);
						if(statsGame == null) {
							statsGame = new PlayerStatsGame();
							statsGame.setPlayerId(gameStatus.getPlayerId());
							statsGame.setNickname(gameStatus.getNickname());
							statsGame.setCampaignId(gameStatus.getCampaignId());
							statsGame.setGlobal(Boolean.FALSE);
							statsGame.setDay(day);
							statsGame.setWeekOfYear(year + "-" + weekOfYear);
							statsGame.setMonthOfYear(year + "-" + monthOfYear);
							statsGameRepository.save(statsGame);
						}
						statsGame.setScore(statsGame.getScore() + delta);
						statsGameRepository.save(statsGame);
					} catch (Exception e) {
						logger.warn("updatePlayerState error:" + e.getMessage());
					}
					
					//update global status 
					JsonNode playerState = gamificationEngineManager.getPlayerStatus(gameId, playerId, "green leaves");
					if(playerState != null) {
						updatePlayerState(playerState, gameStatus);
						gameStatus.setUpdateTime(new Date());
						playerGameStatusRepository.save(gameStatus);
					}
					
					//check recommendation
					if(delta > 0) {
						CampaignSubscription cs = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaign.getCampaignId(), playerId);
						if(cs.hasRecommendationPlayerToDo()) {
							String recommenderPlayerId = (String) cs.getCampaignData().get(Campaign.recommenderPlayerId);
							gamificationEngineManager.sendRecommendation(recommenderPlayerId, gameId);
							cs.getCampaignData().put(Campaign.recommendationPlayerToDo, Boolean.FALSE);
							campaignSubscriptionRepository.save(cs);
						}
					}
				}
			}			
		} catch (Exception e) {
			logger.error(String.format("updatePlayerGameStatus error:%s - %s", e.getMessage(), msg));
		}
	}
	
	private ZonedDateTime getTrackDay(Campaign campaign, CampaignPlayerTrack pt) {		
		ZoneId zoneId = null;
		Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
		if(territory == null) {
			zoneId = ZoneId.systemDefault();
		} else {
			zoneId = ZoneId.of(territory.getTimezone());
		}
		return ZonedDateTime.ofInstant(pt.getStartTime().toInstant(), zoneId);
	}
	
	private void updatePlayerState(JsonNode root, PlayerGameStatus gameStatus) throws Exception {
		//score
		JsonNode concepts = root.findPath("PointConcept");
		for(JsonNode pointConcept : concepts) {
			if(pointConcept.path("name").asText().equals("green leaves")) {
				gameStatus.setScore(pointConcept.path("score").asDouble());
				
				//update generale
				PlayerStatsGame statsGlobal = statsGameRepository.findGlobalByPlayerIdAndCampaignId(
						gameStatus.getPlayerId(), gameStatus.getCampaignId());
				if(statsGlobal == null) {
					statsGlobal = new PlayerStatsGame();
					statsGlobal.setPlayerId(gameStatus.getPlayerId());
					statsGlobal.setNickname(gameStatus.getNickname());
					statsGlobal.setCampaignId(gameStatus.getCampaignId());
					statsGlobal.setGlobal(Boolean.TRUE);
					statsGameRepository.save(statsGlobal);
				}
				statsGlobal.setScore(pointConcept.path("score").asDouble());
				statsGameRepository.save(statsGlobal);
				
			}
		}
		//level
		gameStatus.getLevel().clear();
		JsonNode levels = root.path("levels");
		for(JsonNode level : levels) {
			if(level.path("pointConcept").asText().equals("green leaves")) {
				gameStatus.getLevel().put("levelName", level.path("levelName").asText());
				gameStatus.getLevel().put("levelValue", level.path("levelValue").asText());
				gameStatus.getLevel().put("startLevelScore", level.path("startLevelScore").asDouble());
				gameStatus.getLevel().put("endLevelScore", level.path("endLevelScore").asDouble());
				gameStatus.getLevel().put("toNextLevel", level.path("toNextLevel").asDouble());
			}
		}
		//badges
		gameStatus.getBadges().clear();
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
			gameStatus.getBadges().add(badgeMap);
		}
	}
}
