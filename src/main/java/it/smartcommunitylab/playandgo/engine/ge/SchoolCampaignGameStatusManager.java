package it.smartcommunitylab.playandgo.engine.ge;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsGame;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;

@Component
public class SchoolCampaignGameStatusManager extends BasicCampaignGameStatusManager {
	private static transient final Logger logger = LoggerFactory.getLogger(SchoolCampaignGameStatusManager.class);
	public static String statsGroupId = "teamId";
	
	@Override
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
					
					Player p = playerRepository.findById(playerId).orElse(null);
					PlayerGameStatus gameStatus = playerGameStatusRepository.findByPlayerIdAndCampaignId(playerId, campaignId);
					if(gameStatus == null) {
						gameStatus = new PlayerGameStatus();
						gameStatus.setPlayerId(playerId);
						gameStatus.setNickname(p.getNickname());
						gameStatus.setCampaignId(campaignId);
						playerGameStatusRepository.save(gameStatus);
					}
					
					//update daily points
					CampaignSubscription cs = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaign.getCampaignId(), playerId);
					String groupId = null;
					if(cs != null) {
						groupId = (String) cs.getCampaignData().get(statsGroupId);
					}
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
							statsGame.setGroupId(groupId);
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
					JsonNode playerState = gamificationEngineManager.getPlayerStatus(playerId, gameId, "green leaves");
					if(playerState != null) {
						updatePlayerState(playerState, gameStatus, groupId);
						gameStatus.setUpdateTime(new Date());
						playerGameStatusRepository.save(gameStatus);
					}					
				}
			}			
		} catch (Exception e) {
			logger.error(String.format("updatePlayerGameStatus error:%s - %s", e.getMessage(), msg));
		}
	}
	
}
