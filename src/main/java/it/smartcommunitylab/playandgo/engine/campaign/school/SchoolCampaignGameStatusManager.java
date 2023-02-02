package it.smartcommunitylab.playandgo.engine.campaign.school;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import it.smartcommunitylab.playandgo.engine.campaign.BasicCampaignGameStatusManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class SchoolCampaignGameStatusManager extends BasicCampaignGameStatusManager {
	private static transient final Logger logger = LoggerFactory.getLogger(SchoolCampaignGameStatusManager.class);
	
	@Autowired
	TrackedInstanceRepository trackedInstanceRepository;
	
	@Override
	public void updatePlayerGameStatus(Map<String, Object> msg) {
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
                    Player p = playerRepository.findById(playerId).orElse(null);
                    
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
                        gameStatus = new PlayerGameStatus();
                        gameStatus.setPlayerId(playerId);
                        gameStatus.setNickname(p.getNickname());
                        gameStatus.setCampaignId(campaignId);
                        playerGameStatusRepository.save(gameStatus);
                        logger.debug("add gameStatus " + gameStatus.getId());
                    }
                    
                    //update daily points
                    ZonedDateTime trackDay = null;
                    if(playerTrack != null) {
                        trackDay = getTrackDay(campaign, playerTrack);
                    } else if(p.getGroup() && Utils.isNotEmpty(trackId)) {
                        trackDay = getTrackDay(campaign, trackId, timestamp);
                    } else { 
                        trackDay = getTrackDay(campaign, timestamp);
                    }
                    
                    //update global status 
                    JsonNode playerState = gamificationEngineManager.getPlayerStatus(playerId, gameId, "green leaves");
                    if(playerState != null) {
                        updatePlayerState(playerState, gameStatus, p, trackDay);
                        gameStatus.setUpdateTime(new Date());
                        playerGameStatusRepository.save(gameStatus);
                        logger.debug("update playerState " + gameStatus.getId());
                    }
                    
                    //check recommendation
                    if(!p.getGroup() && (delta > 0)) {
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
	
	private ZonedDateTime getTrackDay(Campaign campaign, String trackId, long timestamp) {
        ZoneId zoneId = null;
        Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
        if(territory == null) {
            zoneId = ZoneId.systemDefault();
        } else {
            zoneId = ZoneId.of(territory.getTimezone());
        }
        TrackedInstance ti = trackedInstanceRepository.findById(trackId).orElse(null);
        if(ti != null) {
            return ZonedDateTime.ofInstant(ti.getStartTime().toInstant(), zoneId);
        }
        return ZonedDateTime.ofInstant(Utils.getUTCDate(timestamp).toInstant(), zoneId);
    }

    protected void updatePlayerState(JsonNode root, PlayerGameStatus gameStatus, Player p, ZonedDateTime day) throws Exception {
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
                    if(p.getGroup()) {
                        statsGlobal.setGroupId(p.getPlayerId()); 
                    }                    
                    statsGameRepository.save(statsGlobal);
                }
                statsGlobal.setScore(pointConcept.path("score").asDouble());
                statsGameRepository.save(statsGlobal);
                
                //update daily
                String dayString = day.format(dtf);
                PlayerStatsGame statsGame = statsGameRepository.findByPlayerIdAndCampaignIdAndDayAndGlobal(gameStatus.getPlayerId(), gameStatus.getCampaignId(), 
                        dayString, Boolean.FALSE);
                if(statsGame == null) {
                    statsGame = new PlayerStatsGame();
                    statsGame.setPlayerId(gameStatus.getPlayerId());
                    statsGame.setNickname(gameStatus.getNickname());
                    statsGame.setCampaignId(gameStatus.getCampaignId());
                    statsGame.setGlobal(Boolean.FALSE);
                    if(p.getGroup()) {
                        statsGame.setGroupId(p.getPlayerId()); 
                    }                    
                    statsGame.setDay(dayString);
                    statsGame.setWeekOfYear(day.format(dftWeek));
                    statsGame.setMonthOfYear(day.format(dftMonth));
                }
                statsGame.setScore(getDailyScore(pointConcept, dayString));
                statsGameRepository.save(statsGame);                
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
	
	private double getDailyScore(JsonNode pointConcept, String day) {
	    return pointConcept.at("/periods/daily/instances/" + day + "T00:00:00/score").asDouble(0.0);
	}
	
	
}
