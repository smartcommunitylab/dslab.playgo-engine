package it.smartcommunitylab.playandgo.engine.campaign;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.campaign.city.CityGameDataConverter;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
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
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public abstract class BasicCampaignGameStatusManager {
	private static transient final Logger logger = LoggerFactory.getLogger(BasicCampaignGameStatusManager.class);
	
	@Autowired
	protected TerritoryRepository territoryRepository;
	
	@Autowired
	protected CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	protected CampaignRepository campaignRepository;
	
	@Autowired
	protected PlayerRepository playerRepository;
	
	@Autowired
	protected PlayerStatsGameRepository statsGameRepository;
	
	@Autowired
	protected PlayerGameStatusRepository playerGameStatusRepository;
	
	@Autowired
	protected CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	protected GamificationEngineManager gamificationEngineManager;
	
	@Autowired
	CityGameDataConverter gameDataConverter;
	
	ObjectMapper mapper = new ObjectMapper();
	
	protected DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	protected DateTimeFormatter dftWeek = DateTimeFormatter.ofPattern("YYYY-ww", Locale.ITALY);
	protected DateTimeFormatter dftMonth = DateTimeFormatter.ofPattern("yyyy-MM");

	public abstract void updatePlayerGameStatus(Map<String, Object> msg);
	
	protected void updatePlayerGameStatus(Map<String, Object> msg, final String groupIdKey) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> obj = (Map<String, Object>) msg.get("obj");
			if(obj != null) {
				String gameId = (String) obj.get("gameId");
				String playerId = (String) obj.get("playerId");
				double delta = (double) obj.get("delta");
				long timestamp = (long) obj.get("timestamp");
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
						logger.debug("update playerTrack " + playerTrack.getId());
					}
					
					PlayerGameStatus gameStatus = playerGameStatusRepository.findByPlayerIdAndCampaignId(playerId, campaignId);
					if(gameStatus == null) {
						Player p = playerRepository.findById(playerId).orElse(null);
						gameStatus = new PlayerGameStatus();
						gameStatus.setPlayerId(playerId);
						gameStatus.setNickname(p.getNickname());
						gameStatus.setCampaignId(campaignId);
						playerGameStatusRepository.save(gameStatus);
						logger.debug("add gameStatus " + gameStatus.getId());
					}
					
					//update daily points
					String groupId = null;
					if(Utils.isNotEmpty(groupIdKey)) {
	                    CampaignSubscription cs = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaign.getCampaignId(), playerId);
	                    if(cs != null) {
	                        groupId = (String) cs.getCampaignData().get(groupIdKey);
	                    }                   					    
					}
					try {
						ZonedDateTime trackDay = null;
						if(playerTrack != null) {
							trackDay = getTrackDay(campaign, playerTrack);
						} else {
							trackDay = getTrackDay(campaign, timestamp);
						}
						String day = trackDay.format(dtf);
						PlayerStatsGame statsGame = statsGameRepository.findByPlayerIdAndCampaignIdAndDayAndGlobal(playerId, campaignId, 
								day, Boolean.FALSE);
						if(statsGame == null) {
							statsGame = new PlayerStatsGame();
							statsGame.setPlayerId(gameStatus.getPlayerId());
							statsGame.setNickname(gameStatus.getNickname());
							statsGame.setCampaignId(gameStatus.getCampaignId());
							statsGame.setGlobal(Boolean.FALSE);
		                    if(Utils.isNotEmpty(groupId)) {
		                        statsGame.setGroupId(groupId);
		                    }							
							statsGame.setDay(day);
							statsGame.setWeekOfYear(trackDay.format(dftWeek));
							statsGame.setMonthOfYear(trackDay.format(dftMonth));
							statsGameRepository.save(statsGame);
							logger.debug("add statsGame " + statsGame.getId());
						}
						statsGame.setScore(statsGame.getScore() + delta);
						statsGameRepository.save(statsGame);
						logger.debug("update statsGame " + statsGame.getId());
					} catch (Exception e) {
						logger.warn("updatePlayerState error:" + e.getMessage());
					}
					
					//update global status 
					JsonNode playerState = gamificationEngineManager.getPlayerStatus(playerId, gameId, "green leaves");
					if(playerState != null) {
						updatePlayerState(playerState, gameStatus, groupId);
						gameStatus.setUpdateTime(new Date());
						playerGameStatusRepository.save(gameStatus);
						logger.debug("update playerState " + gameStatus.getId());
					}
					
					//check recommendation
					if(delta > 0) {
						CampaignSubscription cs = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaign.getCampaignId(), playerId);
						if((cs != null) && cs.hasRecommendationPlayerToDo()) {
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
	
	protected ZonedDateTime getTrackDay(Campaign campaign, CampaignPlayerTrack pt) {		
		ZoneId zoneId = null;
		Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
		if(territory == null) {
			zoneId = ZoneId.systemDefault();
		} else {
			zoneId = ZoneId.of(territory.getTimezone());
		}
		return ZonedDateTime.ofInstant(pt.getStartTime().toInstant(), zoneId);
	}
	
	protected ZonedDateTime getTrackDay(Campaign campaign, long timestamp) {		
		ZoneId zoneId = null;
		Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
		if(territory == null) {
			zoneId = ZoneId.systemDefault();
		} else {
			zoneId = ZoneId.of(territory.getTimezone());
		}
		return ZonedDateTime.ofInstant(Utils.getUTCDate(timestamp).toInstant(), zoneId);
	}
	
	protected void updatePlayerState(JsonNode root, PlayerGameStatus gameStatus, String groupId) throws Exception {
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
					if(Utils.isNotEmpty(groupId)) {
						statsGlobal.setGroupId(groupId);
					}
					statsGameRepository.save(statsGlobal);
				}
				statsGlobal.setScore(pointConcept.path("score").asDouble());
				statsGameRepository.save(statsGlobal);
				
			}
		}
		//level
		gameStatus.getLevels().clear();
		JsonNode levels = root.path("levels"); 
		gameStatus.getLevels().addAll(gameDataConverter.convertLevels(levels));
		//badges
		gameStatus.getBadges().clear();
		JsonNode badges = root.findPath("BadgeCollectionConcept");
		gameStatus.getBadges().addAll(gameDataConverter.convertBadgeCollection(badges));
	}
}
