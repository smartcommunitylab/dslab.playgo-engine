package it.smartcommunitylab.playandgo.engine.campaign;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.campaign.city.CityGameDataConverter;
import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.ge.model.BadgeCollectionConcept;
import it.smartcommunitylab.playandgo.engine.ge.model.PlayerLevel;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerGameStatusRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsGameRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
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
    protected TrackedInstanceRepository trackedInstanceRepository;
	
	@Autowired
	protected GamificationEngineManager gamificationEngineManager;
	
	@Autowired
	protected CityGameDataConverter gameDataConverter;
	
	@Autowired
	MongoTemplate mongoTemplate;
	
	ObjectMapper mapper = new ObjectMapper();
	
	protected DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	protected DateTimeFormatter dftWeek = DateTimeFormatter.ofPattern("YYYY-ww", Locale.ITALY);
	protected DateTimeFormatter dftMonth = DateTimeFormatter.ofPattern("yyyy-MM");

	@SuppressWarnings("unchecked")
    public void updatePlayerGameStatus(Map<String, Object> msg) {
		try {
			Map<String, Object> obj = (Map<String, Object>) msg.get("obj");
			if(obj != null) {
				String gameId = (String) obj.get("gameId");
				String playerId = (String) obj.get("playerId");
				Map<String, Double> deltaMap = (Map<String, Double>) obj.get("deltaMap");
				double delta = deltaMap.containsKey("green leaves") ? deltaMap.get("green leaves") : 0.0;
				long timestamp = (long) obj.get("timestamp");
				String trackId = null;
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
						logger.info("updatePlayerGameStatus: update playerTrack " + playerTrack.getId());
					}
					
					ZonedDateTime trackDay = null;
					if(playerTrack != null) {
						trackDay = getTrackDay(campaign, playerTrack);
					} else if(p.getGroup() && Utils.isNotEmpty(trackId)) {
						trackDay = getTrackDay(campaign, trackId, timestamp);
					} else {
						trackDay = getTrackDay(campaign, timestamp);
					}
					
					//update game stats and status
					JsonNode playerState = gamificationEngineManager.getPlayerStatus(playerId, gameId, "green leaves");
					updatePlayerState(playerState, p, campaign, trackDay, delta);
					logger.info("updatePlayerGameStatus: update player state and stats " + playerId + " - " + gameId);
					
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
	
    protected void updatePlayerState(JsonNode root, Player p, Campaign c, ZonedDateTime day, double delta) throws Exception {
		FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().upsert(true).returnNew(true);

		//levels
        JsonNode levelsNode = root.path("levels"); 
		List<PlayerLevel> levels = new ArrayList<>();
		if(!levelsNode.isMissingNode()) {
			levels = gameDataConverter.convertLevels(levelsNode);
		}

		//badges
		JsonNode badgesNode = root.findPath("BadgeCollectionConcept");
		List<BadgeCollectionConcept> badges = new ArrayList<>();
		if(!badgesNode.isMissingNode()) {
			badges = gameDataConverter.convertBadgeCollection(badgesNode);
		}

        //score
        JsonNode concepts = root.findPath("PointConcept");
        for(JsonNode pointConcept : concepts) {
            if(pointConcept.path("name").asText().equals("green leaves")) {
				double score = pointConcept.path("score").asDouble();

				//update status
				Query gameStatusQuery = new Query(new Criteria("playerId").is(p.getPlayerId()).and("campaignId").is(c.getCampaignId())); 
				Update gameStatusUpdate = upsertGameStatus(p, c, levels, badges, score);
				mongoTemplate.findAndModify(gameStatusQuery, gameStatusUpdate, findAndModifyOptions, PlayerGameStatus.class);

				//update global
				Query globalStatsQuery = new Query(new Criteria("playerId").is(p.getPlayerId()).and("campaignId").is(c.getCampaignId())
					.and("global").is(Boolean.TRUE));
				Update globalStatsUpdate = upsertGameStats(p, c, Boolean.TRUE, null, null, null, score, false);
				mongoTemplate.findAndModify(globalStatsQuery, globalStatsUpdate, findAndModifyOptions, PlayerStatsGame.class);
				
				//update daily
				String dayString = day.format(dtf);
				String weekOfYear = day.format(dftWeek);
				String monthOfYear = day.format(dftMonth);
				double dailyScore = 0.0;
				boolean isDelta = false;
				JsonNode node = pointConcept.at("/periods/daily/instances/" + dayString + "T00:00:00/score");
				if(node.isMissingNode()) {
					isDelta = true;
					dailyScore = delta;
				} else {
					dailyScore = node.asDouble();
				}
				Query dailyStatsQuery = new Query(new Criteria("playerId").is(p.getPlayerId()).and("campaignId").is(c.getCampaignId())
					.and("global").is(Boolean.FALSE).and("day").is(dayString));
				Update dailyStatsUpdate = upsertGameStats(p, c, Boolean.FALSE, dayString, weekOfYear, monthOfYear, dailyScore, isDelta);
				mongoTemplate.findAndModify(dailyStatsQuery, dailyStatsUpdate, findAndModifyOptions, PlayerStatsGame.class);                
            }
        }
	}
    
    private void setDailyScore(PlayerStatsGame statsGame, JsonNode pointConcept, String day, double delta) {
        String path = "/periods/daily/instances/" + day + "T00:00:00/score";
        JsonNode node = pointConcept.at(path);
        if(node.isMissingNode()) {
            statsGame.setScore(statsGame.getScore() + delta);
        } else {
            statsGame.setScore(node.asDouble());
        }
    }

	private Update upsertGameStatus(Player p, Campaign c, List<PlayerLevel> levels, List<BadgeCollectionConcept> badges, double score) {
		Update update = new Update();
		update.setOnInsert("playerId", p.getPlayerId());
		update.setOnInsert("nickname", p.getNickname());
		update.setOnInsert("campaignId", c.getCampaignId());
		update.set("levels", levels);
		update.set("badges", badges);
		update.set("updateTime", new Date());
		update.set("score", score);
		return update;
	}

	private Update upsertGameStats(Player p, Campaign c, Boolean global, String day, String weekOfYear, String monthOfYear, double score, boolean isDelta) {
		Update update = new Update();
		update.setOnInsert("playerId", p.getPlayerId());
		update.setOnInsert("nickname", p.getNickname());
		update.setOnInsert("campaignId", c.getCampaignId());
		update.setOnInsert("global", global);
		if(Utils.isNotEmpty(day)) update.setOnInsert("day", day);
		if(Utils.isNotEmpty(monthOfYear)) update.setOnInsert("monthOfYear", monthOfYear);
		if(Utils.isNotEmpty(weekOfYear)) update.setOnInsert("weekOfYear", weekOfYear);
		if(p.getGroup()) update.setOnInsert("groupId", p.getPlayerId());
		if(isDelta) {
			update.inc("score", score);
		} else {
			update.set("score", score);
		}
		return update;
	}
 
}
